package org.develar.mapsforgeTileServer;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalNotification;
import com.luciad.imageio.webp.WebPUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import org.jetbrains.annotations.NotNull;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.netty.handler.codec.http.HttpHeaders.Names.*;

@ChannelHandler.Sharable
public class TileHttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
  final static Logger LOG = Logger.getLogger(TileHttpRequestHandler.class.getName());

  // http://wiki.openstreetmap.org/wiki/Slippy_map_tilenames
  private static final Pattern MAP_TILE_NAME_PATTERN = Pattern.compile("^/(\\d+)/(\\d+)/(\\d+)(?:\\.(png|webp))?(?:\\?theme=(\\w+))?");

  private final MapsforgeTileServer tileServer;
  private final LoadingCache<TileRequest, RenderedTile> tileMemoryCache;

  static {
    ImageIO.setUseCache(false);
  }

  private final ThreadLocal<Renderer> threadLocalRenderer = new ThreadLocal<Renderer>() {
    @Override
    protected Renderer initialValue() {
      return new Renderer();
    }
  };

  public TileHttpRequestHandler(@NotNull MapsforgeTileServer tileServer, @NotNull Options options, int executorCount, @NotNull List<Runnable> shutdownHooks) throws IOException {
    this.tileServer = tileServer;

    File cacheFile = options.cacheFile;
    DBMaker dbMaker = DBMaker.newFileDB(cacheFile).transactionDisable().mmapFileEnablePartial().cacheDisable();
    if (options.maxFileCacheSize > 0) {
      dbMaker.sizeLimit(options.maxFileCacheSize);
    }
    DB db = createCacheDb(cacheFile, dbMaker);
    HTreeMap<TileRequest, RenderedTile> tileFileCache = db.createHashMap("tiles")
      .keySerializer(new TileRequest.TileRequestSerializer())
      .valueSerializer(new RenderedTileSerializer())
      .makeOrGet();

    BlockingQueue<RemovalNotification<TileRequest, RenderedTile>> flushQueue = new LinkedBlockingQueue<>();

    Thread flushThread = new Thread(() -> {
      while (true) {
        try {
          RemovalNotification<TileRequest, RenderedTile> removalNotification = flushQueue.take();
          tileFileCache.put(removalNotification.getKey(), removalNotification.getValue());
        }
        catch (InterruptedException ignored) {
          break;
        }
      }
    }, "Memory to file cache writer");
    flushThread.setPriority(Thread.MIN_PRIORITY);
    flushThread.start();

    tileMemoryCache = CacheBuilder.newBuilder()
      .concurrencyLevel(executorCount)
      .weigher((TileRequest key, RenderedTile value) -> TileRequest.WEIGHT + value.computeWeight())
      .maximumWeight(Runtime.getRuntime().freeMemory() - (64 * 1024 * 1024) /* leave 64MB for another stuff */)
      .removalListener((RemovalNotification<TileRequest, RenderedTile> notification) -> {
        if (notification.wasEvicted()) {
          flushQueue.add(notification);
        }
      })
      .build(new CacheLoader<TileRequest, RenderedTile>() {
        @Override
        public RenderedTile load(@NotNull TileRequest tile) throws Exception {
          RenderedTile renderedTile = tileFileCache.get(tile);
          return renderedTile == null ? renderTile(tile) : renderedTile;
        }
      });

    shutdownHooks.add(new Runnable() {
      @Override
      public void run() {
        LOG.info("Stop 'Memory to file cache writer' thread");
        flushThread.interrupt();
      }
    });
    shutdownHooks.add(new Runnable() {
      @Override
      public void run() {
        LOG.info("Flush unwritten data");
        try {
          tileMemoryCache.asMap().entrySet().parallelStream().forEach(entry -> tileFileCache.put(entry.getKey(), entry.getValue()));
        }
        finally {
          db.close();
        }
      }
    });
  }

  @NotNull
  private static DB createCacheDb(@NotNull File cacheFile, @NotNull DBMaker dbMaker) throws IOException {
    try {
      return dbMaker.make();
    }
    catch (Throwable e) {
      LOG.log(Level.SEVERE, "Cannot open file cache db, db will be recreated", e);
      //noinspection ResultOfMethodCallIgnored
      cacheFile.delete();
      Files.deleteIfExists(Paths.get(cacheFile.getPath(), ".p"));
      return dbMaker.make();
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext context, Throwable cause) throws Exception {
    if (cause instanceof IOException && cause.getMessage().equals("Connection reset by peer")) {
      // ignore Connection reset by peer
      return;
    }

    LOG.log(Level.SEVERE, cause.getMessage(), cause);
  }

  private static boolean checkClientCache(@NotNull HttpRequest request, long lastModified, @NotNull String etag) {
    String ifModifiedSince = request.headers().get(IF_MODIFIED_SINCE);
    if (ifModifiedSince != null && !ifModifiedSince.isEmpty()) {
      try {
        if (Responses.parseTime(ifModifiedSince) >= lastModified) {
          return true;
        }
      }
      catch (DateTimeParseException ignored) {
      }
    }

    return etag.equals(request.headers().get(IF_NONE_MATCH));
  }

  @Override
  protected void channelRead0(ChannelHandlerContext context, FullHttpRequest request) throws Exception {
    Matcher matcher = MAP_TILE_NAME_PATTERN.matcher(request.getUri());
    Channel channel = context.channel();
    if (!matcher.find()) {
      Responses.sendStatus(HttpResponseStatus.BAD_REQUEST, channel, request);
      return;
    }

    byte zoom = Byte.parseByte(matcher.group(1));
    long x = Long.parseLong(matcher.group(2));
    long y = Long.parseLong(matcher.group(3));

    ImageFormat imageFormat = ImageFormat.fromName(matcher.group(4));
    boolean useVaryAccept = imageFormat == null;
    if (useVaryAccept) {
      String accept = request.headers().get(ACCEPT);
      imageFormat = accept != null && accept.contains(ImageFormat.WEBP.getContentType()) ? ImageFormat.WEBP : ImageFormat.PNG;
    }

    RenderedTile renderedTile = tileMemoryCache.get(new TileRequest(x, y, zoom, (byte)imageFormat.ordinal()));
    if (checkClientCache(request, renderedTile.lastModified, renderedTile.etag)) {
      Responses.send(Responses.response(HttpResponseStatus.NOT_MODIFIED), channel, request);
      return;
    }

    boolean isHeadRequest = request.getMethod() == HttpMethod.HEAD;
    HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, isHeadRequest ? Unpooled.EMPTY_BUFFER : Unpooled.wrappedBuffer(renderedTile.data));
    response.headers().set(CONTENT_TYPE, imageFormat.getContentType());
    // default cache for one day
    response.headers().set(CACHE_CONTROL, "public, max-age=" + (60 * 60 * 24));
    response.headers().set(ETAG, renderedTile.etag);
    response.headers().set(LAST_MODIFIED, Responses.formatTime(renderedTile.lastModified));
    Responses.addCommonHeaders(response);
    if (useVaryAccept) {
      response.headers().add(VARY, "Accept");
    }

    boolean keepAlive = Responses.addKeepAliveIfNeed(response, request);
    if (!isHeadRequest) {
      HttpHeaders.setContentLength(response, renderedTile.data.length);
    }

    ChannelFuture future = channel.writeAndFlush(response);
    if (!keepAlive) {
      future.addListener(ChannelFutureListener.CLOSE);
    }
  }

  @NotNull
  private RenderedTile renderTile(@NotNull TileRequest tile) throws IOException {
    Renderer rendererManager = threadLocalRenderer.get();
    TileRenderer renderer = rendererManager.getTileRenderer(tile, tileServer);
    BufferedImage bufferedImage = renderer.render(tile);
    byte[] bytes;
    if (tile.getImageFormat() == ImageFormat.WEBP) {
      bytes = WebPUtil.encode(bufferedImage);
    }
    else {
      ByteArrayOutputStream out = new ByteArrayOutputStream(8 * 1024);
      ImageIO.write(bufferedImage, tile.getImageFormat().getFormatName(), out);
      bytes = out.toByteArray();
      out.close();
    }
    return new RenderedTile(bytes, Math.floorDiv(System.currentTimeMillis(), 1000), renderer.computeETag(tile, rendererManager.stringBuilder));
  }
}

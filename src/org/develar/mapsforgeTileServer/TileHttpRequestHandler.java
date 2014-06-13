package org.develar.mapsforgeTileServer;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.luciad.imageio.webp.WebP;
import com.luciad.imageio.webp.WebPWriteParam;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.util.internal.FastThreadLocal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.layer.renderer.DatabaseRenderer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.netty.handler.codec.http.HttpHeaders.Names.*;
import static org.develar.mapsforgeTileServer.MapsforgeTileServer.LOG;

@ChannelHandler.Sharable
public class TileHttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
  private static final WebPWriteParam WRITE_PARAM = new WebPWriteParam(Locale.ENGLISH);

  // http://wiki.openstreetmap.org/wiki/Slippy_map_tilenames
  private static final Pattern MAP_TILE_NAME_PATTERN = Pattern.compile("^/(\\d+)/(\\d+)/(\\d+)(?:\\.(png|webp))?(?:\\?theme=(\\w+))?");

  private final MapsforgeTileServer tileServer;
  private final LoadingCache<TileRequest, RenderedTile> tileCache;

  static {
    ImageIO.setUseCache(false);
  }

  private final FastThreadLocal<Renderer> threadLocalRenderer = new FastThreadLocal<Renderer>() {
    @Override
    protected Renderer initialValue() {
      return new Renderer();
    }
  };

  private final DatabaseRenderer.TileCacheInfoProvider tileCacheInfoProvider;

  public TileHttpRequestHandler(@NotNull MapsforgeTileServer tileServer, @Nullable FileCacheManager fileCacheManager, int executorCount, long maxMemoryCacheSize, @NotNull List<Runnable> shutdownHooks) {
    this.tileServer = tileServer;

    CacheBuilder<TileRequest, RenderedTile> cacheBuilder = CacheBuilder.newBuilder()
      .concurrencyLevel(executorCount)
      .weigher((TileRequest key, RenderedTile value) -> TileRequest.WEIGHT + value.computeWeight())
      .maximumWeight(maxMemoryCacheSize);
    if (fileCacheManager == null) {
      tileCache = cacheBuilder.build(new CacheLoader<TileRequest, RenderedTile>() {
        @Override
        public RenderedTile load(@NotNull TileRequest tile) throws Exception {
          return renderTile(tile);
        }
      });
    }
    else {
      tileCache = fileCacheManager.configureMemoryCache(cacheBuilder).build(new CacheLoader<TileRequest, RenderedTile>() {
        @Override
        public RenderedTile load(@NotNull TileRequest tile) throws Exception {
          RenderedTile renderedTile = fileCacheManager.get(tile);
          return renderedTile == null ? renderTile(tile) : renderedTile;
        }
      });

      shutdownHooks.add(() -> {
        LOG.info("Flush unwritten data");
        fileCacheManager.close(tileCache.asMap());
      });
    }

    tileCacheInfoProvider = (tile, rendererJob) -> tileCache.asMap().containsKey((TileRequest)tile);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext context, Throwable cause) throws Exception {
    if (cause instanceof IOException && cause.getMessage().equals("Connection reset by peer")) {
      // ignore Connection reset by peer
      return;
    }

    LOG.error(cause.getMessage(), cause);
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

    long maxTileNumber = Tile.getMaxTileNumber(zoom);
    if (x > maxTileNumber || y > maxTileNumber) {
      Responses.send(Responses.response(HttpResponseStatus.BAD_REQUEST), channel, request);
      return;
    }

    ImageFormat imageFormat = ImageFormat.fromName(matcher.group(4));
    boolean useVaryAccept = imageFormat == null;
    if (useVaryAccept) {
      String accept = request.headers().get(ACCEPT);
      imageFormat = accept != null && accept.contains(ImageFormat.WEBP.getContentType()) ? ImageFormat.WEBP : ImageFormat.PNG;
    }

    RenderedTile renderedTile;
    try {
      renderedTile = tileCache.get(new TileRequest(x, y, zoom, (byte)imageFormat.ordinal()));
    }
    catch (UncheckedExecutionException e) {
      if (e.getCause() instanceof TileNotFound) {
        Responses.send(Responses.response(HttpResponseStatus.NOT_FOUND), channel, request);
      }
      else {
        Responses.send(Responses.response(HttpResponseStatus.INTERNAL_SERVER_ERROR), channel, request);
        LOG.error(e.getMessage(), e);
      }
      return;
    }

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
    TileRenderer renderer = rendererManager.getTileRenderer(tile, tileServer, tileCacheInfoProvider);
    if (renderer == null) {
      throw TileNotFound.INSTANCE;
    }

    BufferedImage bufferedImage = renderer.render(tile);
    byte[] bytes = tile.getImageFormat() == ImageFormat.WEBP ? WebP.encode(WRITE_PARAM, bufferedImage) : encodePng(bufferedImage);
    return new RenderedTile(bytes, Math.floorDiv(System.currentTimeMillis(), 1000), renderer.computeETag(tile, rendererManager.stringBuilder));
  }

  private static byte[] encodePng(@NotNull BufferedImage bufferedImage) throws IOException {
    byte[] bytes;ByteArrayOutputStream out = new ByteArrayOutputStream(8 * 1024);
    ImageIO.write(bufferedImage, "png", out);
    bytes = out.toByteArray();
    out.close();
    return bytes;
  }

  private static class TileNotFound extends RuntimeException {
    private static final TileNotFound INSTANCE = new TileNotFound();
  }
}

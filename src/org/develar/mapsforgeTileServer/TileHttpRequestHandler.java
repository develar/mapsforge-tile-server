package org.develar.mapsforgeTileServer;

import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import org.jetbrains.annotations.NotNull;
import org.mapsforge.core.model.Tile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeParseException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaders.Names.VARY;

@ChannelHandler.Sharable
public class TileHttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
  private final static Logger LOG = Logger.getLogger(TileHttpRequestHandler.class.getName());

  // http://wiki.openstreetmap.org/wiki/Slippy_map_tilenames
  private static final Pattern MAP_TILE_NAME_PATTERN = Pattern.compile("^/(\\d+)/(\\d+)/(\\d+)(?:\\.(png|webp))?(?:\\?theme=(\\w+))?");

  private final MapsforgeTileServer tileServer;
  private final Cache<Tile, RenderedTile> tileCache;

  static {
    ImageIO.setUseCache(false);
  }

  private final ThreadLocal<Renderer> threadLocalRenderer = new ThreadLocal<Renderer>() {
    @Override
    protected Renderer initialValue() {
      return new Renderer();
    }
  };

  public TileHttpRequestHandler(@NotNull MapsforgeTileServer tileServer, @NotNull Cache<Tile, RenderedTile> tileCache) {
    this.tileServer = tileServer;
    this.tileCache = tileCache;
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext context, Throwable cause) throws Exception {
    if (cause instanceof IOException && cause.getMessage().equals("Connection reset by peer")) {
      // ignore Connection reset by peer
      return;
    }

    LOG.log(Level.SEVERE, cause.getMessage(), cause);
  }

  private static boolean checkCache(@NotNull HttpRequest request, @NotNull Channel channel, long lastModified) {
    String ifModifiedSince = request.headers().get(HttpHeaders.Names.IF_MODIFIED_SINCE);
    if (!Strings.isNullOrEmpty(ifModifiedSince)) {
      try {
        if (Responses.parseTime(ifModifiedSince) >= lastModified) {
          Responses.send(Responses.response(HttpResponseStatus.NOT_MODIFIED), channel, request);
          return true;
        }
      }
      catch (DateTimeParseException ignored) {
      }
    }
    return false;
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
      String accept = request.headers().get(HttpHeaders.Names.ACCEPT);
      imageFormat = accept != null && accept.contains(ImageFormat.WEBP.getContentType()) ? ImageFormat.WEBP : ImageFormat.PNG;
    }

    Tile tile = new TileEx(x, y, zoom, imageFormat);
    RenderedTile renderedTile = tileCache.getIfPresent(tile);
    if (renderedTile == null) {
      BufferedImage bufferedImage = threadLocalRenderer.get().render(tile, tileServer);
      ByteArrayOutputStream out = new ByteArrayOutputStream(8 * 1024);
      ImageIO.write(bufferedImage, imageFormat.getFormatName(), out);
      byte[] bytes = out.toByteArray();
      out.close();

      tileCache.put(tile, renderedTile = new RenderedTile(bytes));
    }
    else if (checkCache(request, channel, renderedTile.lastModified)) {
      return;
    }

    boolean isHeadRequest = request.getMethod() == HttpMethod.HEAD;
    HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, isHeadRequest ? Unpooled.EMPTY_BUFFER : Unpooled.wrappedBuffer(renderedTile.getData()));
    response.headers().add(CONTENT_TYPE, imageFormat.getContentType());
    response.headers().set(HttpHeaders.Names.LAST_MODIFIED, Responses.formatTime(renderedTile.lastModified));
    Responses.addCommonHeaders(response);
    if (useVaryAccept) {
      response.headers().add(VARY, "Accept");
    }

    boolean keepAlive = Responses.addKeepAliveIfNeed(response, request);
    if (!isHeadRequest) {
      HttpHeaders.setContentLength(response, renderedTile.getData().length);
    }

    ChannelFuture future = channel.writeAndFlush(response);
    if (!keepAlive) {
      future.addListener(ChannelFutureListener.CLOSE);
    }
  }
}

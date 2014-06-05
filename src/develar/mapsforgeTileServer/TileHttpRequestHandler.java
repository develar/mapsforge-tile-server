package develar.mapsforgeTileServer;

import com.google.common.cache.Cache;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import org.jetbrains.annotations.NotNull;
import org.mapsforge.core.model.Tile;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;

@ChannelHandler.Sharable
public class TileHttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
  private final static Logger LOG = Logger.getLogger(TileHttpRequestHandler.class.getName());

  // http://wiki.openstreetmap.org/wiki/Slippy_map_tilenames
  private static final Pattern MAP_TILE_NAME_PATTERN = Pattern.compile("^/(\\d+)/(\\d+)/(\\d+)(?:\\.(png|webp))?(?:\\?theme=(\\w+))?");

  private final MapsforgeTileServer tileServer;
  private final Cache<Tile, byte[]> tileCache;

  private final ThreadLocal<Renderer> threadLocalRenderer = new ThreadLocal<Renderer>() {
    @Override
    protected Renderer initialValue() {
      return new Renderer();
    }
  };

  public TileHttpRequestHandler(@NotNull MapsforgeTileServer tileServer, @NotNull Cache<Tile, byte[]> tileCache) {
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

    Tile tile = new Tile(x, y, zoom);
    byte[] bytes = tileCache.getIfPresent(tile);
    if (bytes == null) {
      bytes = threadLocalRenderer.get().render(tile, tileServer);
      tileCache.put(tile, bytes);
    }

    HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.wrappedBuffer(bytes));
    response.headers().add(CONTENT_TYPE, "image/png");
    Responses.addCommonHeaders(response);
    boolean keepAlive = Responses.addKeepAliveIfNeed(response, request);
    HttpHeaders.setContentLength(response, bytes.length);

    ChannelFuture future = channel.writeAndFlush(response);
    if (!keepAlive) {
      future.addListener(ChannelFutureListener.CLOSE);
    }
  }
}

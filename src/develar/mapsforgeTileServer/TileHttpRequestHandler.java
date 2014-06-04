package develar.mapsforgeTileServer;

import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import org.jetbrains.annotations.NotNull;
import org.mapsforge.core.model.Tile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;

@ChannelHandler.Sharable
public class TileHttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
  // http://wiki.openstreetmap.org/wiki/Slippy_map_tilenames
  private static final Pattern MAP_TILE_NAME_PATTERN = Pattern.compile("^/(\\d+)/(\\d+)/(\\d+)");

  private final TileRenderer tileRenderer;

  public TileHttpRequestHandler(@NotNull TileRenderer tileRenderer) {
    this.tileRenderer = tileRenderer;
  }

  @Override
  protected void channelRead0(ChannelHandlerContext context, FullHttpRequest request) throws Exception {
    Matcher matcher = MAP_TILE_NAME_PATTERN.matcher(request.getUri());
    if (!matcher.find()) {
      Responses.sendStatus(HttpResponseStatus.BAD_REQUEST, context.channel(), request);
      return;
    }

    byte zoom = Byte.parseByte(matcher.group(1));
    long x = Long.parseLong(matcher.group(2));
    long y = Long.parseLong(matcher.group(3));

    BufferedImage bufferedImage = tileRenderer.render(new Tile(x, y, zoom));
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ImageIO.write(bufferedImage, "png", out);
    byte[] bytes = out.toByteArray();
    out.close();

    HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.wrappedBuffer(bytes));
    response.headers().add(CONTENT_TYPE, "image/png");
    Responses.addCommonHeaders(response);
    boolean keepAlive = Responses.addKeepAliveIfNeed(response, request);
    HttpHeaders.setContentLength(response, bytes.length);

    ChannelFuture future = context.channel().writeAndFlush(response);
    if (!keepAlive) {
      future.addListener(ChannelFutureListener.CLOSE);
    }
  }
}

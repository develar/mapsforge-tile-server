package develar.mapsforgeTileServer;

import com.google.common.cache.CacheBuilder;
import com.sampullara.cli.Args;
import com.sampullara.cli.Argument;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import org.jetbrains.annotations.NotNull;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.awt.AwtGraphicFactory;
import org.mapsforge.map.model.Model;
import org.mapsforge.map.rendertheme.ExternalRenderTheme;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

public class MapsforgeTileServer {
  public static final GraphicFactory GRAPHIC_FACTORY = AwtGraphicFactory.INSTANCE;

  public static class Options {
    @Argument(alias = "m", description = "Map files (see http://www.openandromaps.org/en/downloads)", required = true)
    public File[] maps;

    @Argument(alias = "t", description = "Render theme (see http://www.openandromaps.org/en/legend)", required = true)
    public File theme;

    @Argument(alias = "ms", description = "Memory cache spec, see http://docs.guava-libraries.googlecode.com/git/javadoc/com/google/common/cache/CacheBuilder.html", value = "maximumSize=1024")
    public String memoryCacheSpec;
    @Argument(description = "SQLite file cache location", value = "cache.sqlite")
    public int cacheFile;
  }

  public static void main(String[] args) throws InterruptedException, FileNotFoundException {
    Options options = new Options();
    Args.parseOrExit(options, args);

    File[] maps = options.maps;
    validateMapFiles(maps);

    validateFile(options.theme);

    ExternalRenderTheme xmlRenderTheme = new ExternalRenderTheme(options.theme);

    Model model = new Model();
    BoundingBox result = null;
    List<TileRenderer> tileLayers = new ArrayList<>();
    for (File mapFile : maps) {
      TileRendererImpl tileRendererLayer = new TileRendererImpl(model.displayModel);
      tileRendererLayer.setMapFile(mapFile);
      tileRendererLayer.setXmlRenderTheme(xmlRenderTheme);

      BoundingBox boundingBox = tileRendererLayer.getMapDatabase().getMapFileInfo().boundingBox;
      result = result == null ? boundingBox : result.extend(boundingBox);
      tileLayers.add(tileRendererLayer);
    }

    EventLoopGroup bossGroup = new NioEventLoopGroup();
    EventLoopGroup workerGroup = new NioEventLoopGroup();
    final TileHttpRequestHandler tileHttpRequestHandler = new TileHttpRequestHandler(tileLayers.get(0), CacheBuilder.from(options.memoryCacheSpec).<Tile, byte[]>build());

    try {
      ServerBootstrap serverBootstrap = new ServerBootstrap();
      serverBootstrap.group(bossGroup, workerGroup)
        .channel(NioServerSocketChannel.class)
        .childHandler(new ChannelInitializer<Channel>() {
          @Override
          public void initChannel(Channel channel) throws Exception {
            channel.pipeline().addLast(new HttpRequestDecoder(), new HttpObjectAggregator(1048576 * 10), new HttpResponseEncoder());
            channel.pipeline().addLast(tileHttpRequestHandler);
          }
        })
        .childOption(ChannelOption.SO_KEEPALIVE, true)
        .childOption(ChannelOption.TCP_NODELAY, true);

      serverBootstrap.bind(17778).syncUninterruptibly().channel().closeFuture().sync();
    }
    finally {
      workerGroup.shutdownGracefully();
      bossGroup.shutdownGracefully();
    }
  }

  private static void validateMapFiles(@NotNull File[] maps) {
    for (File mapFile : maps) {
      validateFile(mapFile);
    }
  }

  private static void validateFile(@NotNull File mapFile) {
    if (!mapFile.exists()) {
      throw new IllegalArgumentException("File does not exist: " + mapFile);
    }
    else if (!mapFile.isFile()) {
      throw new IllegalArgumentException("Not a file: " + mapFile);
    }
    else if (!mapFile.canRead()) {
      throw new IllegalArgumentException("Cannot read file: " + mapFile);
    }
  }
}

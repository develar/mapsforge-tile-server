package develar.mapsforgeTileServer;

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
import org.mapsforge.map.awt.AwtGraphicFactory;
import org.mapsforge.map.layer.cache.FileSystemTileCache;
import org.mapsforge.map.layer.cache.InMemoryTileCache;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.cache.TwoLevelTileCache;
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

    @Argument(alias = "mc", description = "The maximum number of entries in memory cache, minimum 64", value = "1024")
    public int memoryCacheCapacity;
    @Argument(alias = "fc", description = "The maximum number of entries in file cache, 0 to disable", value = "1048576")
    public int fileCacheCapacity;
  }

  public static void main(String[] args) throws InterruptedException, FileNotFoundException {
    Options options = new Options();
    Args.parseOrExit(options, args);

    File[] maps = options.maps;
    validateMapFiles(maps);

    validateFile(options.theme);

    ExternalRenderTheme xmlRenderTheme = new ExternalRenderTheme(options.theme);

    TileCache tileCache = createTileCache(options);

    Model model = new Model();
    BoundingBox result = null;
    List<TileRenderer> tileLayers = new ArrayList<>();
    for (File mapFile : maps) {
      TileRendererImpl tileRendererLayer = new TileRendererImpl(tileCache, model.displayModel);
      tileRendererLayer.setMapFile(mapFile);
      tileRendererLayer.setXmlRenderTheme(xmlRenderTheme);

      BoundingBox boundingBox = tileRendererLayer.getMapDatabase().getMapFileInfo().boundingBox;
      result = result == null ? boundingBox : result.extend(boundingBox);
      tileLayers.add(tileRendererLayer);
    }

    EventLoopGroup bossGroup = new NioEventLoopGroup();
    EventLoopGroup workerGroup = new NioEventLoopGroup();
    final TileHttpRequestHandler tileHttpRequestHandler = new TileHttpRequestHandler(tileLayers.get(0));

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

  @NotNull
  private static TileCache createTileCache(@NotNull Options options) {
    TileCache tileCache = new InMemoryTileCache(Math.max(options.memoryCacheCapacity, 64));
    if (options.fileCacheCapacity > 0) {
      File cacheDirectory = new File(System.getProperty("java.io.tmpdir"), "mapsforge-tile-server");
      tileCache = new TwoLevelTileCache(tileCache, new FileSystemTileCache(options.fileCacheCapacity, cacheDirectory, GRAPHIC_FACTORY));
    }
    return tileCache;
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

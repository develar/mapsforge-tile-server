package org.develar.mapsforgeTileServer;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.util.concurrent.MultithreadEventExecutorGroup;
import org.jetbrains.annotations.NotNull;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.map.awt.AwtGraphicFactory;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.rendertheme.ExternalRenderTheme;
import org.mapsforge.map.rendertheme.XmlRenderTheme;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MapsforgeTileServer {
  private final static Logger LOG = Logger.getLogger(MapsforgeTileServer.class.getName());
  static final GraphicFactory GRAPHIC_FACTORY = AwtGraphicFactory.INSTANCE;

  final List<File> maps;
  final Map<String, XmlRenderTheme> renderThemes;
  final DisplayModel displayModel;
  final XmlRenderTheme defaultRenderTheme;

  public MapsforgeTileServer(@NotNull List<File> maps, @NotNull Map<String, XmlRenderTheme> renderThemes) {
    this.maps = maps;
    this.renderThemes = renderThemes;

    displayModel = new DisplayModel();
    XmlRenderTheme defaultRenderTheme = renderThemes.get("elevate");
    if (defaultRenderTheme == null) {
      defaultRenderTheme = renderThemes.values().iterator().next();
    }
    this.defaultRenderTheme = defaultRenderTheme;
  }

  public static void main(String[] args) throws IOException {
    Options options = new Options();
    try {
      new CmdLineParser(options).parseArgument(args);
    }
    catch (CmdLineException e) {
      System.err.print(e.getMessage());
      System.exit(64);
    }

    List<File> mapFiles = new ArrayList<>(options.maps.length);
    for (Path mapFile : options.maps) {
      if (!Files.exists(mapFile)) {
        throw new IllegalArgumentException("File does not exist: " + mapFile);
      }
      else if (Files.isDirectory(mapFile)) {
        Files.walk(mapFile).filter(path -> !Files.isDirectory(path) && path.getFileName().toString().endsWith(".map")).forEachOrdered(path -> mapFiles.add(path.toFile()));
      }
      else if (!Files.isReadable(mapFile)) {
        throw new IllegalArgumentException("Cannot read file: " + mapFile);
      }
    }

    Path theme = options.theme;
    Map<String, XmlRenderTheme> renderThemes = new HashMap<>();
    if (Files.isDirectory(theme)) {
      Files.walk(theme, 2).filter(path -> !Files.isDirectory(path) && path.getFileName().toString().endsWith(".xml")).forEachOrdered(path -> {
        try {
          addRenderTheme(path, renderThemes);
        }
        catch (FileNotFoundException e) {
          LOG.log(Level.SEVERE, e.getMessage(), e);
        }
      });
    }
    else {
      addRenderTheme(theme, renderThemes);
    }

    if (renderThemes.isEmpty()) {
      LOG.log(Level.SEVERE, "No render themes specified");
      return;
    }

    new MapsforgeTileServer(mapFiles, renderThemes).startServer(options);
  }

  private static void addRenderTheme(@NotNull Path path, @NotNull Map<String, XmlRenderTheme> renderThemes) throws FileNotFoundException {
    String fileName = path.getFileName().toString();
    renderThemes.put(fileName.substring(0, fileName.length() - ".xml".length()).toLowerCase(Locale.ENGLISH), new ExternalRenderTheme(path.toFile()));
  }

  private static long getAvailableMemory() {
    Runtime runtime = Runtime.getRuntime();
    long totalMemory = runtime.totalMemory(); // current heap allocated to the VM process
    long freeMemory = runtime.freeMemory(); // out of the current heap, how much is free
    long maxMemory = runtime.maxMemory(); // Max heap VM can use e.g. Xmx setting
    long usedMemory = totalMemory - freeMemory; // how much of the current heap the VM is using
    // available memory i.e. Maximum heap size minus the current amount used
    return maxMemory - usedMemory;
  }

  private void startServer(@NotNull Options options) throws IOException {
    Runtime runtime = Runtime.getRuntime();
    long freeMemory = runtime.freeMemory();
    long maxMemoryCacheSize = getAvailableMemory() - (64 * 1024 * 1024) /* leave 64MB for another stuff */;
    if (maxMemoryCacheSize <= 0) {
      LOG.severe("Memory not enough, current free memory " + freeMemory + ", total memory " + runtime.totalMemory() + ", max memory " + runtime.maxMemory());
      return;
    }

    boolean isLinux = System.getProperty("os.name").toLowerCase(Locale.US).startsWith("linux");
    final EventLoopGroup bossGroup = isLinux ? new EpollEventLoopGroup() : new NioEventLoopGroup();
    final EventLoopGroup workerGroup = isLinux ? new EpollEventLoopGroup() : new NioEventLoopGroup();
    final ChannelRegistrar channelRegistrar = new ChannelRegistrar();

    List<Runnable> shutdownHooks = new ArrayList<>(4);
    shutdownHooks.add(() -> {
      LOG.info("Close opened connections");
      channelRegistrar.close(false);
    });
    shutdownHooks.add(() -> {
      LOG.info("Shutdown server");
      workerGroup.shutdownGracefully();
      bossGroup.shutdownGracefully();
    });

    final TileHttpRequestHandler tileHttpRequestHandler = new TileHttpRequestHandler(this, options, ((MultithreadEventExecutorGroup)workerGroup).executorCount(), maxMemoryCacheSize, shutdownHooks);
    ServerBootstrap serverBootstrap = new ServerBootstrap();
    serverBootstrap.group(bossGroup, workerGroup)
      .channel(isLinux ? EpollServerSocketChannel.class : NioServerSocketChannel.class)
      .option(ChannelOption.SO_BACKLOG, 100)
      .childHandler(new ChannelInitializer<Channel>() {
        @Override
        public void initChannel(Channel channel) throws Exception {
          channel.pipeline().addLast(channelRegistrar);
          channel.pipeline().addLast(new HttpRequestDecoder(), new HttpObjectAggregator(1048576 * 10), new HttpResponseEncoder());
          channel.pipeline().addLast(tileHttpRequestHandler);
        }
      })
      .childOption(ChannelOption.SO_KEEPALIVE, true)
      .childOption(ChannelOption.TCP_NODELAY, true);

    InetSocketAddress address = options.host == null || options.host.isEmpty() ? new InetSocketAddress(InetAddress.getLoopbackAddress(), options.port) : new InetSocketAddress(options.host, options.port);
    Channel serverChannel = serverBootstrap.bind(address).syncUninterruptibly().channel();
    channelRegistrar.add(serverChannel);

    Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
      @Override
      public void run() {
        for (Runnable shutdownHook : shutdownHooks) {
          try {
            shutdownHook.run();
          }
          catch (Throwable e) {
            LOG.log(Level.SEVERE, e.getMessage(), e);
          }
        }
      }
    }));

    LOG.info("Listening " + address.getHostName() + ":" + address.getPort());
    serverChannel.closeFuture().syncUninterruptibly();
  }
}
package org.develar.mapsforgeTileServer

import com.google.common.collect.ImmutableMap
import com.google.common.collect.Iterators
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.epoll.EpollEventLoopGroup
import io.netty.channel.epoll.EpollServerSocketChannel
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpRequestDecoder
import io.netty.handler.codec.http.HttpResponseEncoder
import io.netty.util.concurrent.Future
import io.netty.util.concurrent.MultithreadEventExecutorGroup
import org.kohsuke.args4j.CmdLineException
import org.kohsuke.args4j.CmdLineParser
import org.mapsforge.core.graphics.GraphicFactory
import org.mapsforge.map.model.DisplayModel
import org.mapsforge.map.rendertheme.ExternalRenderTheme
import org.mapsforge.map.rendertheme.renderinstruction.Symbol
import org.mapsforge.map.rendertheme.rule.*
import org.slf4j.LoggerFactory
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException

import org.develar.mapsforgeTileServer.pixi.*

import java.io.File
import java.io.IOException
import java.io.OutputStreamWriter
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer
import java.util.function.Predicate
import org.mapsforge.core.graphics.Bitmap
import org.slf4j.Logger

public val LOG:Logger = LoggerFactory.getLogger(javaClass<MapsforgeTileServer>())
val AWT_GRAPHIC_FACTORY: GraphicFactory = MyAwtGraphicFactory()

throws(javaClass<IOException>())
public fun main(args: Array<String>) {
  val options = Options()
  //printUsage(options);
  try {
    CmdLineParser(options).parseArgument(args.toList())
  }
  catch (e: CmdLineException) {
    System.err.print(e.getMessage())
    System.exit(64)
  }


  val maps = ArrayList<File>(options.maps!!.size)
  processPaths(options.maps!!, ".map", Integer.MAX_VALUE, object: Consumer<Path> {
    override fun accept(path: Path) {
      maps.add(path.toFile())
    }
  })

  if (maps.isEmpty()) {
    LOG.error("No map specified")
    return
  }

  val mapsforgeTileServer: MapsforgeTileServer
  try {
    mapsforgeTileServer = MapsforgeTileServer(maps, options.themes!!)
  }
  catch (e: IllegalStateException) {
    LOG.error(e.getMessage())
    return
  }


  mapsforgeTileServer.startServer(options)
}

private val RENDER_THEME_FACTORY = object : RenderThemeFactory {
  class AwtSymbol(graphicFactory: GraphicFactory?, displayModel: DisplayModel, qName: String, pullParser: XmlPullParser, relativePathPrefix: String) : Symbol(graphicFactory, displayModel, qName, pullParser) {
    private val _bitmap = createBitmap(relativePathPrefix, src)

    override fun getBitmap(): Bitmap? = _bitmap
  }

  override fun create(renderThemeBuilder: RenderThemeBuilder): RenderTheme {
    return RenderTheme(renderThemeBuilder, UglyGuavaCacheBuilderJavaWrapper.createCache(), UglyGuavaCacheBuilderJavaWrapper.createCache())
  }

  throws(javaClass<IOException>(), javaClass<XmlPullParserException>())
  override fun createSymbol(graphicFactory: GraphicFactory?, displayModel: DisplayModel, qName: String, pullParser: XmlPullParser, relativePathPrefix: String): Symbol {
    if (graphicFactory == AWT_GRAPHIC_FACTORY) {
      return AwtSymbol(graphicFactory, displayModel, qName, pullParser, relativePathPrefix)
    }
    else {
      return PixiSymbol(displayModel, qName, pullParser, relativePathPrefix)
    }
  }
}

throws(javaClass<IOException>())
private fun processPaths(paths: Array<Path>, ext: String, maxDepth: Int, action: Consumer<Path>) {
  for (specifiedPath in paths) {
    if (!Files.exists(specifiedPath)) {
      throw IllegalArgumentException("File does not exist: " + specifiedPath)
    }
    else
      if (!Files.isReadable(specifiedPath)) {
        throw IllegalArgumentException("Cannot read file: " + specifiedPath)
      }
      else
        if (Files.isDirectory(specifiedPath)) {
          Files.walk(specifiedPath, maxDepth).filter(object: Predicate<Path> {
            override fun test(path: Path): Boolean = !Files.isDirectory(path) && path.getFileName().toString().endsWith(ext)
          }).forEachOrdered(action)
        }
        else {
          action.accept(specifiedPath)
        }
  }
}

SuppressWarnings("UnusedDeclaration")
private fun printUsage(options: Options) {
  CmdLineParser(options).printUsage(OutputStreamWriter(System.out), object : ResourceBundle() {
    private val data = ImmutableMap.Builder<String, String>().put("FILE", "<path>").put("PATH", "<path>").put("VAL", "<string>").put("N", " <int>").build()

    override fun handleGetObject(key: String): Any {
      return data.get(key) ?: key
    }

    override fun getKeys(): Enumeration<String> {
      return Iterators.asEnumeration(data.keySet().iterator())
    }
  })
}

throws(javaClass<IOException>(), javaClass<XmlPullParserException>())
private fun createRenderTheme(graphicFactory: GraphicFactory, displayModel: DisplayModel, xmlRenderTheme: ExternalRenderTheme): RenderTheme {
  val renderTheme = RenderThemeHandler.getRenderTheme(graphicFactory, displayModel, xmlRenderTheme, RENDER_THEME_FACTORY)
  renderTheme.scaleTextSize(1f)
  return renderTheme
}

private fun getAvailableMemory(): Long {
  val runtime = Runtime.getRuntime()
  val totalMemory = runtime.totalMemory() // current heap allocated to the VM process
  val freeMemory = runtime.freeMemory() // out of the current heap, how much is free
  val maxMemory = runtime.maxMemory() // Max heap VM can use e.g. Xmx setting
  val usedMemory = totalMemory - freeMemory // how much of the current heap the VM is using
  // available memory i.e. Maximum heap size minus the current amount used
  return maxMemory - usedMemory
}

public class MapsforgeTileServer(val maps: List<File>, renderThemeFiles: Array<Path>) {
  val renderThemes = LinkedHashMap<String, RenderThemeItem>()
  val displayModel: DisplayModel = DisplayModel()
  val defaultRenderTheme: RenderThemeItem;

  {
    processPaths(renderThemeFiles, ".xml", 2, object: Consumer<Path> {
      override fun accept(path: Path) {
        addRenderTheme(path, displayModel)
      }
    })

    if (renderThemes.isEmpty()) {
      throw IllegalStateException("No render theme specified")
    }

    var themeName = "elevate"
    var defaultRenderTheme = renderThemes.get(themeName)
    if (defaultRenderTheme == null) {
      themeName = renderThemes.keySet().iterator().next()
      defaultRenderTheme = renderThemes.get(themeName)
    }

    LOG.info("Use " + themeName + " as default theme")

    this.defaultRenderTheme = defaultRenderTheme!!
  }

  throws(javaClass<IOException>(), javaClass<XmlPullParserException>())
  private fun addRenderTheme(path: Path, displayModel: DisplayModel) {
    val fileName = path.getFileName().toString()
    val name = fileName.substring(0, fileName.length() - ".xml".length()).toLowerCase(Locale.ENGLISH)
    val xmlRenderTheme = ExternalRenderTheme(path.toFile())
    val etag = name + "@" + java.lang.Long.toUnsignedString(Files.getLastModifiedTime(path).toMillis(), 32)

    val vectorRenderTheme = createRenderTheme(PixiGraphicFactory.INSTANCE, displayModel, xmlRenderTheme)
    // scale depends on zoom, but we cannot set it on each "render tile" invocation - render theme must be immutable,
    // it is client reponsibility to do scaling
    vectorRenderTheme.scaleStrokeWidth(1f)

    renderThemes.put(name, RenderThemeItem(createRenderTheme(AWT_GRAPHIC_FACTORY, displayModel, xmlRenderTheme), vectorRenderTheme, etag))
  }

  throws(javaClass<IOException>())
  fun startServer(options: Options) {
    val runtime = Runtime.getRuntime()
    val freeMemory = runtime.freeMemory()
    val maxMemoryCacheSize = getAvailableMemory() - (64 * 1024 * 1024).toLong() /* leave 64MB for another stuff */
    if (maxMemoryCacheSize <= 0) {
      LOG.error("Memory not enough, current free memory " + freeMemory + ", total memory " + runtime.totalMemory() + ", max memory " + runtime.maxMemory())
      return
    }

    val isLinux = System.getProperty("os.name")!!.toLowerCase(Locale.US).startsWith("linux")
    val eventGroup = if (isLinux) EpollEventLoopGroup() else NioEventLoopGroup()
    val channelRegistrar = ChannelRegistrar()

    val eventGroupShutdownFeature = AtomicReference<Future<*>>()
    val shutdownHooks = ArrayList<Runnable>(4)
    shutdownHooks.add(object: Runnable {
      override fun run() {
        LOG.info("Shutdown server");
        try {
          channelRegistrar.closeAndSyncUninterruptibly();
        }
        finally {
          if (!eventGroupShutdownFeature.compareAndSet(null, eventGroup.shutdownGracefully())) {
            LOG.error("ereventGroupShutdownFeature was already set");
          }
        }
      }
    })

    val executorCount = (eventGroup : MultithreadEventExecutorGroup).executorCount()
    val fileCacheManager = if (options.maxFileCacheSize == 0.0) null else FileCacheManager(options, executorCount, shutdownHooks)
    val tileHttpRequestHandler = TileHttpRequestHandler(this, fileCacheManager, executorCount, maxMemoryCacheSize, shutdownHooks)

    // task "sync eventGroupShutdownFeature only" must be last
    shutdownHooks.add(object: Runnable {
      override fun run() {
        eventGroupShutdownFeature.getAndSet(null)!!.syncUninterruptibly()
      }
    })

    val serverBootstrap = ServerBootstrap()
    serverBootstrap.group(eventGroup).channel(if (isLinux) javaClass<EpollServerSocketChannel>() else javaClass<NioServerSocketChannel>()).childHandler(object : ChannelInitializer<Channel>() {
      throws(javaClass<Exception>())
      override fun initChannel(channel: Channel) {
        channel.pipeline().addLast(channelRegistrar)
        channel.pipeline().addLast(HttpRequestDecoder(), HttpObjectAggregator(1048576 * 10), HttpResponseEncoder())
        channel.pipeline().addLast(tileHttpRequestHandler)
      }
    }).childOption<Boolean>(ChannelOption.SO_KEEPALIVE, true).childOption<Boolean>(ChannelOption.TCP_NODELAY, true)

    val address = if (options.host.orEmpty().isEmpty()) InetSocketAddress(InetAddress.getLoopbackAddress(), options.port) else InetSocketAddress(options.host!!, options.port)
    val serverChannel = serverBootstrap.bind(address).syncUninterruptibly().channel()
    channelRegistrar.addServerChannel(serverChannel)

    Runtime.getRuntime().addShutdownHook(Thread())

    LOG.info("Listening " + address.getHostName() + ":" + address.getPort())
    serverChannel.closeFuture().syncUninterruptibly()
  }
}
package org.develar.mapsforgeTileServer

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import com.google.common.util.concurrent.UncheckedExecutionException
import com.luciad.imageio.webp.WebP
import com.luciad.imageio.webp.WebPWriteParam
import io.netty.buffer.Unpooled
import io.netty.channel.*
import io.netty.handler.codec.http.*
import org.develar.mapsforgeTileServer.pixi.PixiShape
import org.mapsforge.core.model.Tile
import org.mapsforge.map.layer.renderer.DatabaseRenderer

import javax.imageio.ImageIO
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.Locale
import java.util.regex.Pattern

import io.netty.handler.codec.http.HttpHeaders.Names.*
import org.mapsforge.map.layer.renderer.RendererJob
import com.google.common.cache.Weigher
import io.netty.util.concurrent.FastThreadLocal
import org.develar.mapsforgeTileServer.http.checkCache
import org.develar.mapsforgeTileServer.http.sendStatus
import org.develar.mapsforgeTileServer.http.send
import org.develar.mapsforgeTileServer.http.response
import org.develar.mapsforgeTileServer.http.formatTime
import org.develar.mapsforgeTileServer.http.addCommonHeaders
import org.develar.mapsforgeTileServer.http.addKeepAliveIfNeed
import org.develar.mapsforgeTileServer.http.sendFile
import java.io.File

class TileNotFound() : RuntimeException() {
  class object {
    val INSTANCE = TileNotFound()
  }
}

private val WRITE_PARAM = WebPWriteParam(Locale.ENGLISH)

// http://wiki.openstreetmap.org/wiki/Slippy_map_tilenames
private val MAP_TILE_NAME_PATTERN = Pattern.compile("^/(\\d+)/(\\d+)/(\\d+)(?:\\.(png|webp|v))?(?:\\?theme=(\\w+))?")

private fun checkClientCache(request: HttpRequest, lastModified: Long, etag: String): Boolean {
  return checkCache(request, lastModified) || etag == request.headers().get(IF_NONE_MATCH)
}

private fun encodePng(bufferedImage: BufferedImage): ByteArray {
  val bytes: ByteArray
  val out = ByteArrayOutputStream(8 * 1024)
  ImageIO.write(bufferedImage, "png", out)
  bytes = out.toByteArray()
  out.close()
  return bytes
}

ChannelHandler.Sharable
class TileHttpRequestHandler(private val tileServer: MapsforgeTileServer, fileCacheManager: FileCacheManager?, executorCount: Int, maxMemoryCacheSize: Long, shutdownHooks: MutableList<()->Unit>) : SimpleChannelInboundHandler<FullHttpRequest>() {
  private val tileCache: LoadingCache<TileRequest, RenderedTile>

  private val fontsDir = File(System.getProperty("mts.fontsDir")!!)

  private val threadLocalRenderer = object : FastThreadLocal<Renderer>() {
    override fun initialValue(): Renderer {
      return Renderer(tileServer)
    }
  }

  private val tileCacheInfoProvider: DatabaseRenderer.TileCacheInfoProvider

  {
    val cacheBuilder = CacheBuilder.newBuilder()
            .concurrencyLevel(executorCount)
            .weigher(object: Weigher<TileRequest, RenderedTile> {
              override fun weigh(key: TileRequest, value: RenderedTile): Int = TILE_REQUEST_WEIGHT + value.computeWeight()
            })
            .maximumWeight(maxMemoryCacheSize)
    if (fileCacheManager == null) {
      tileCache = cacheBuilder.build(object : CacheLoader<TileRequest, RenderedTile>() {
        throws(javaClass<Exception>())
        override fun load(tile: TileRequest): RenderedTile {
          return renderTile(tile)
        }
      })
    }
    else {
      tileCache = fileCacheManager.configureMemoryCache(cacheBuilder).build(object : CacheLoader<TileRequest, RenderedTile>() {
        throws(javaClass<Exception>())
        override fun load(tile: TileRequest): RenderedTile {
          val renderedTile = fileCacheManager.get(tile)
          return renderedTile ?: renderTile(tile)
        }
      })

      shutdownHooks.add {
        LOG.info("Flush unwritten data");
        fileCacheManager.close(tileCache.asMap());
      }
    }

    tileCacheInfoProvider = object: DatabaseRenderer.TileCacheInfoProvider {
      override fun contains(tile: Tile, rendererJob: RendererJob?): Boolean = tileCache.asMap().containsKey(tile as TileRequest)
    }
  }

  class object {
    {
      ImageIO.setUseCache(false)
    }
  }

  override fun exceptionCaught(context: ChannelHandlerContext, cause: Throwable) {
    if (cause is IOException && cause.getMessage() == "Connection reset by peer") {
      // ignore Connection reset by peer
      return
    }

    LOG.error(cause.getMessage(), cause)
  }

  override fun channelRead0(context: ChannelHandlerContext, request: FullHttpRequest) {
    val uri = request.uri()
    val matcher = MAP_TILE_NAME_PATTERN.matcher(uri)
    val channel = context.channel()
    if (!matcher.find()) {
      val file: File?
      val fontsDirName = "fonts/"
      if (uri.startsWith(fontsDirName, 1)) {
        file = File(fontsDir, uri.substring(fontsDirName.length + 1))
      }
      else {
        file = tileServer.renderThemeManager.requestToFile(uri)
      }

      if (file != null) {
        sendFile(request, channel, file)
        return
      }

      sendStatus(HttpResponseStatus.NOT_FOUND, channel, request)
      return
    }

    val zoom = java.lang.Byte.parseByte(matcher.group(1)!!)
    val x = Integer.parseUnsignedInt(matcher.group(2))
    val y = Integer.parseUnsignedInt(matcher.group(3))

    val maxTileNumber = Tile.getMaxTileNumber(zoom)
    if (x > maxTileNumber || y > maxTileNumber) {
      send(response(HttpResponseStatus.BAD_REQUEST), channel, request)
      return
    }

    var imageFormat = imageFormat(matcher.group(4))
    val useVaryAccept = imageFormat == null
    if (useVaryAccept) {
      val accept = request.headers().get(ACCEPT)
      imageFormat = if (accept != null && accept.contains(ImageFormat.WEBP.getContentType())) ImageFormat.WEBP else ImageFormat.PNG
    }

    val renderedTile: RenderedTile

    val tile = TileRequest(x, y, zoom, imageFormat!!.ordinal().toByte())
    if (imageFormat == ImageFormat.VECTOR) {
      val rendererManager = threadLocalRenderer.get()
      val renderer = rendererManager.getTileRenderer(tile, tileServer, tileCacheInfoProvider)
      if (renderer == null) {
        send(response(HttpResponseStatus.NOT_FOUND), channel, request)
        return
      }

      val bitmap = renderer.renderVector(tile) as PixiShape
      renderedTile = RenderedTile(bitmap.build(), Math.floorDiv(System.currentTimeMillis(), 1000), "")
    }
    else {
      try {
        renderedTile = tileCache.get(tile)
      }
      catch (e: UncheckedExecutionException) {
        if (e.getCause() is TileNotFound) {
          send(response(HttpResponseStatus.NOT_FOUND), channel, request)
        }
        else {
          send(response(HttpResponseStatus.INTERNAL_SERVER_ERROR), channel, request)
          LOG.error(e.getMessage(), e)
        }
        return
      }

    }

    if (checkCache(request, renderedTile.lastModified) || renderedTile.etag == request.headers().get(IF_NONE_MATCH)) {
      send(response(HttpResponseStatus.NOT_MODIFIED), channel, request)
      return
    }

    val isHeadRequest = request.method() == HttpMethod.HEAD
    val response = DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, if (isHeadRequest) Unpooled.EMPTY_BUFFER else Unpooled.wrappedBuffer(renderedTile.data))
    //noinspection ConstantConditions
    response.headers().set(CONTENT_TYPE, imageFormat!!.getContentType())
    // default cache for one day
    if (imageFormat != ImageFormat.VECTOR) {
      response.headers().set(CACHE_CONTROL, "public, max-age=" + (60 * 60 * 24))
      response.headers().set(ETAG, renderedTile.etag)
    }
    response.headers().set(LAST_MODIFIED, formatTime(renderedTile.lastModified))
    addCommonHeaders(response)
    if (useVaryAccept) {
      response.headers().add(VARY, "Accept")
    }

    val keepAlive = addKeepAliveIfNeed(response, request)
    if (!isHeadRequest) {
      HttpHeaders.setContentLength(response, renderedTile.data.size.toLong())
    }

    val future = channel.writeAndFlush(response)
    if (!keepAlive) {
      future.addListener(ChannelFutureListener.CLOSE)
    }
  }

  throws(javaClass<IOException>())
  private fun renderTile(tile: TileRequest): RenderedTile {
    val rendererManager = threadLocalRenderer.get()
    val renderer = rendererManager.getTileRenderer(tile, tileServer, tileCacheInfoProvider)
    if (renderer == null) {
      throw TileNotFound.INSTANCE
    }

    val bufferedImage = renderer.render(tile)
    val bytes = if (tile.getImageFormat() == ImageFormat.WEBP) WebP.encode(WRITE_PARAM, bufferedImage) else encodePng(bufferedImage)
    return RenderedTile(bytes, Math.floorDiv(System.currentTimeMillis(), 1000), renderer.computeETag(tile, rendererManager.stringBuilder))
  }
}

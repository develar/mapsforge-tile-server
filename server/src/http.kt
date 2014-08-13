package org.develar.mapsforgeTileServer.http

import io.netty.handler.codec.http.HttpRequest
import io.netty.channel.Channel
import java.io.File
import java.io.RandomAccessFile
import java.io.FileNotFoundException
import javax.activation.MimetypesFileTypeMap
import io.netty.handler.codec.http.DefaultHttpResponse
import io.netty.handler.codec.http.HttpHeaders
import io.netty.handler.codec.http.HttpHeaders.Names.IF_MODIFIED_SINCE
import java.time.format.DateTimeParseException
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.HttpVersion
import io.netty.handler.codec.http.HttpMethod
import io.netty.handler.ssl.SslHandler
import io.netty.channel.DefaultFileRegion
import io.netty.handler.stream.ChunkedFile
import io.netty.handler.codec.http.LastHttpContent
import io.netty.channel.ChannelFutureListener
import org.develar.mapsforgeTileServer.ImageFormat
import io.netty.handler.codec.http.HttpHeaders.Names.ACCEPT

private val FILE_MIMETYPE_MAP = MimetypesFileTypeMap()

fun getContentType(path:String):String {
  return FILE_MIMETYPE_MAP.getContentType(path)
}

fun isWebpSupported(request:HttpRequest):Boolean = request.headers().get(ACCEPT)?.contains(ImageFormat.WEBP.getContentType()) ?: false

fun checkCache(request:HttpRequest, lastModified:Long):Boolean {
  val ifModifiedSince = request.headers().get(IF_MODIFIED_SINCE)
  if (ifModifiedSince != null && !ifModifiedSince.isEmpty()) {
    try {
      if (parseTime(ifModifiedSince) >= lastModified) {
        return true
      }
    }
    catch (ignored:DateTimeParseException) {
    }
  }
  return false
}

fun sendFile(request:HttpRequest, channel:Channel, file:File) {
  if (checkCache(request, file.lastModified())) {
    send(response(HttpResponseStatus.NOT_MODIFIED), channel, request)
    return
  }

  val response = DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK)
  response.headers().add(HttpHeaders.Names.CONTENT_TYPE, getContentType(file.getPath()))
  addCommonHeaders(response)
  response.headers().set(HttpHeaders.Names.CACHE_CONTROL, "private, must-revalidate")
  response.headers().set(HttpHeaders.Names.LAST_MODIFIED, formatTime(file.lastModified()))

  val keepAlive = addKeepAliveIfNeed(response, request)

  var fileWillBeClosed = false
  val raf:RandomAccessFile
  try {
    raf = RandomAccessFile(file, "r")
  }
  catch (ignored:FileNotFoundException) {
    send(response(HttpResponseStatus.NOT_FOUND), channel, request)
    return
  }

  try {
    val fileLength = raf.length()
    if (request.method() != HttpMethod.HEAD) {
      HttpHeaders.setContentLength(response, fileLength)
    }

    channel.write(response)
    if (request.method() != HttpMethod.HEAD) {
      if (channel.pipeline().get(javaClass<SslHandler>()) == null) {
        // no encryption - use zero-copy
        channel.write(DefaultFileRegion(raf.getChannel(), 0, fileLength))
      }
      else {
        // cannot use zero-copy with HTTPS
        channel.write(ChunkedFile(raf))
      }
    }
    fileWillBeClosed = true
  }
  finally {
    if (!fileWillBeClosed) {
      raf.close()
    }
  }

  val future = channel.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT)
  if (!keepAlive) {
    future.addListener(ChannelFutureListener.CLOSE)
  }
}

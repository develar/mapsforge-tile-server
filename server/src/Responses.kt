/*
 * Copyright 2000-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.develar.mapsforgeTileServer.http

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.*
import io.netty.handler.codec.http.HttpHeaders.Names.*
import io.netty.util.CharsetUtil
import java.nio.charset.Charset
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField
import java.util.Locale

private val DATE_FORMAT = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US)

private val GMT_ZONE_ID = ZoneId.of("GMT")

public fun formatTime(epochSecond: Long): String {
  return DATE_FORMAT.format(ZonedDateTime.ofInstant(Instant.ofEpochSecond(epochSecond), GMT_ZONE_ID))
}

public fun parseTime(time: String): Long {
  val temporalAccessor = DATE_FORMAT.parse(time)
  if (ChronoField.INSTANT_SECONDS.isSupportedBy(temporalAccessor)) {
    return temporalAccessor.getLong(ChronoField.INSTANT_SECONDS)
  }
  else {
    return ZonedDateTime.from(temporalAccessor).toEpochSecond()
  }
}

public fun response(status: HttpResponseStatus): FullHttpResponse {
  return DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, Unpooled.EMPTY_BUFFER)
}

public fun response(contentType: String?, content: ByteBuf?): HttpResponse {
  val response = DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, content ?: Unpooled.EMPTY_BUFFER)
  if (contentType != null) {
    response.headers().add(CONTENT_TYPE, contentType)
  }
  return response
}

public fun addAllowAnyOrigin(response: HttpResponse) {
  response.headers().add(ACCESS_CONTROL_ALLOW_ORIGIN, "*")
}

public fun addDate(response: HttpResponse) {
  if (!response.headers().contains(DATE)) {
    addDate(response, ZonedDateTime.now(GMT_ZONE_ID))
  }
}

public fun addDate(response: HttpResponse, temporal: ZonedDateTime) {
  response.headers().set(DATE, DATE_FORMAT.format(temporal))
}

public fun addNoCache(response: HttpResponse) {
  response.headers().add(CACHE_CONTROL, "no-cache, no-store, must-revalidate, max-age=0")
  response.headers().add(PRAGMA, "no-cache")
}

public fun addServer(response: HttpResponse) {
  response.headers().add(SERVER, "MapsforgeTileServer")
}

public fun send(response: HttpResponse, channel: Channel, request: HttpRequest?) {
  if (response.status() != HttpResponseStatus.NOT_MODIFIED && !HttpHeaders.isContentLengthSet(response)) {
    HttpHeaders.setContentLength(response, (if (response is FullHttpResponse) (response).content().readableBytes() else 0).toLong())
  }

  addCommonHeaders(response)
  send(response, channel, request != null && !addKeepAliveIfNeed(response, request))
}

public fun addKeepAliveIfNeed(response: HttpResponse, request: HttpRequest): Boolean {
  if (HttpHeaders.isKeepAlive(request)) {
    HttpHeaders.setKeepAlive(response, true)
    return true
  }
  return false
}

public fun addCommonHeaders(response: HttpResponse) {
  addServer(response)
  addDate(response)
  addAllowAnyOrigin(response)
}

public fun send(content: CharSequence, channel: Channel, request: HttpRequest?) {
  send(content, CharsetUtil.US_ASCII, channel, request)
}

public fun send(content: CharSequence, charset: Charset, channel: Channel, request: HttpRequest?) {
  send(DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.copiedBuffer(content, charset)), channel, request)
}

private fun send(response: HttpResponse, channel: Channel, close: Boolean) {
  if (!channel.isActive()) {
    return
  }

  val future = channel.write(response)
  if (response !is FullHttpResponse) {
    channel.write(LastHttpContent.EMPTY_LAST_CONTENT)
  }
  channel.flush()
  if (close) {
    future.addListener(ChannelFutureListener.CLOSE)
  }
}

public fun sendStatus(responseStatus: HttpResponseStatus, channel: Channel) {
  sendStatus(responseStatus, channel, null)
}

public fun sendStatus(responseStatus: HttpResponseStatus, channel: Channel, request: HttpRequest?) {
  sendStatus(responseStatus, channel, null, request)
}

public fun sendStatus(responseStatus: HttpResponseStatus, channel: Channel, description: String?, request: HttpRequest?) {
  send(createStatusResponse(responseStatus, request, description), channel, request)
}

private fun createStatusResponse(responseStatus: HttpResponseStatus, request: HttpRequest?, description: String?): HttpResponse {
  if (request != null && request.method() == HttpMethod.HEAD) {
    return response(responseStatus)
  }

  val builder = StringBuilder()
  val message = responseStatus.toString()
  builder.append("<!doctype html><title>").append(message).append("</title>").append("<h1 style=\"text-align: center\">").append(message).append("</h1>")
  if (description != null) {
    builder.append("<p>").append(description).append("</p>")
  }
  builder.append("<hr/><p style=\"text-align: center\">").append("MapsforgeTileServer").append("</p>")

  val response = DefaultFullHttpResponse(HttpVersion.HTTP_1_1, responseStatus, Unpooled.copiedBuffer(builder, CharsetUtil.UTF_8))
  response.headers().set(CONTENT_TYPE, "text/html")
  return response
}

public fun sendOptionsResponse(allowHeaders: String, request: HttpRequest, context: ChannelHandlerContext) {
  val response = response(HttpResponseStatus.OK)
  response.headers().set(ACCESS_CONTROL_ALLOW_METHODS, allowHeaders)
  response.headers().set(ALLOW, allowHeaders)
  send(response, context.channel(), request)
}
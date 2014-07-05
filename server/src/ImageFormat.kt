package org.develar.mapsforgeTileServer

import org.develar.mapsforgeTileServer.ImageFormat.WEBP
import org.develar.mapsforgeTileServer.ImageFormat.PNG
import org.develar.mapsforgeTileServer.ImageFormat.VECTOR

public fun imageFormat(name: String?): ImageFormat? {
  if (name == null || name.isEmpty()) {
    return null
  }
  else {
    return if (name.charAt(0) == 'w') WEBP else (if (name.charAt(0) == 'p') PNG else VECTOR)
  }
}

public enum class ImageFormat {
  WEBP
  PNG
  VECTOR

  fun getContentType() = when (this) {
    WEBP -> "image/webp"
    PNG -> "image/png"
    VECTOR -> "application/octet-stream"
    else -> throw IllegalStateException()
  }

  fun getFormatName() = if (this == WEBP) "webp" else if (this == PNG) "png" else "pixi"
}
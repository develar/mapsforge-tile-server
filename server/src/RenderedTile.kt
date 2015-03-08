package org.develar.mapsforgeTileServer

import java.io.Serializable

public class RenderedTile(public val data: ByteArray, public val lastModified: Long, public val etag: String) : Serializable {
  public fun computeWeight(): Int = data.size() + 4 + etag.length()
}

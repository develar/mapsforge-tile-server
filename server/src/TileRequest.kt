package org.develar.mapsforgeTileServer

import org.mapdb.DataInput2
import org.mapdb.DataOutput2
import org.mapdb.Serializer
import org.mapsforge.core.model.Tile
import java.io.DataInput
import java.io.DataOutput
import java.io.Serializable

private val DEFAULT_TILE_SIZE = 256
public val TILE_REQUEST_WEIGHT: Int = 8 + 8 + 1 + 1

class TileRequest(tileX: Int, tileY: Int, zoomLevel: Byte, private val imageFormat: Byte) : Tile(tileX, tileY, zoomLevel, DEFAULT_TILE_SIZE), Serializable {
  override fun createNeighbourTile(y: Int, x: Int): Tile {
    return TileRequest(x, y, zoomLevel, imageFormat)
  }

  public fun getImageFormat(): ImageFormat {
    return if (imageFormat == 0.toByte()) ImageFormat.WEBP else ImageFormat.PNG
  }

  override fun hashCode(): Int {
    return 31 * super<Tile>.hashCode() + imageFormat.toInt()
  }

  override fun equals(other: Any?): Boolean {
    return super<Tile>.equals(other) && (other as TileRequest).imageFormat == imageFormat
  }

  class TileRequestSerializer() : Serializer<TileRequest>, Serializable {
    override fun serialize(out: DataOutput, value: TileRequest) {
      DataOutput2.packInt(out, value.tileX)
      DataOutput2.packInt(out, value.tileY)
      out.write(value.zoomLevel.toInt())
      out.write(value.imageFormat.toInt())
    }

    override fun deserialize(`in`: DataInput, available: Int): TileRequest {
      return TileRequest(DataInput2.unpackInt(`in`), DataInput2.unpackInt(`in`), `in`.readByte(), `in`.readByte())
    }

    override fun fixedSize(): Int {
      return -1
    }
  }
}

package org.develar.mapsforgeTileServer

import org.mapsforge.core.model.BoundingBox
import org.mapsforge.core.model.Tile
import org.mapsforge.map.layer.renderer.DatabaseRenderer

fun intersects(minLongitude: Double, maxLongitude: Double, minLatitude: Double, maxLatitude: Double, mapBoundingBox: BoundingBox): Boolean {
  var tw = maxLongitude - minLongitude
  var th = maxLatitude - minLatitude
  var rw = mapBoundingBox.getLongitudeSpan()
  var rh = mapBoundingBox.getLatitudeSpan()
  if (rw <= 0 || rh <= 0 || tw <= 0 || th <= 0) {
    return false
  }

  val tx = minLongitude
  val ty = minLatitude
  val rx = mapBoundingBox.minLongitude
  val ry = mapBoundingBox.minLatitude
  rw += rx
  rh += ry
  tw += tx
  th += ty
  // overflow || intersect
  return ((rw < rx || rw > minLongitude) && (rh < ry || rh > minLatitude) && (tw < minLongitude || tw > rx) && (th < minLatitude || th > ry))
}

fun tileToLon(x: Int, z: Byte): Double {
  return x / Math.pow(2.0, z.toDouble()) * 360.0 - 180
}

fun tileToLat(y: Int, z: Byte): Double {
  val n = Math.PI - (2.0 * Math.PI * y.toDouble()) / Math.pow(2.0, z.toDouble())
  return Math.toDegrees(Math.atan(Math.sinh(n)))
}

public class Renderer(tileServer: MapsforgeTileServer) {
  private val tileRenderers: Array<TileRenderer?>? = arrayOfNulls(tileServer.maps.size())

  public val stringBuilder: StringBuilder = StringBuilder()

  public fun getTileRenderer(tile: Tile, tileServer: MapsforgeTileServer, tileCacheInfoProvider: DatabaseRenderer.TileCacheInfoProvider): TileRenderer? {
    // todo see http://localhost:6090/3/4/2.png - we should render not only Osterreich/Monaco (Alps.map), but Germany too (Germany.map)
    val y = tile.tileY
    val north = tileToLat(y, tile.zoomLevel)
    val south = tileToLat(y + 1, tile.zoomLevel)
    val x = tile.tileX
    val west = tileToLon(x, tile.zoomLevel)
    val east = tileToLon(x + 1, tile.zoomLevel)

    val maps = tileServer.maps
    for (i in 0..maps.size() - 1) {
      val mapFile = maps.get(i)
      var tileRenderer = tileRenderers!![i]
      if (tileRenderer == null) {
        tileRenderer = TileRenderer(tileServer.displayModel, mapFile, tileServer.defaultRenderTheme, tileCacheInfoProvider)
        tileRenderers[i] = tileRenderer;
      }

      val mapBoundingBox = tileRenderer!!.boundingBox
      if (intersects(west, east, south, north, mapBoundingBox)) {
        return tileRenderer
      }
    }

    return null
  }
}
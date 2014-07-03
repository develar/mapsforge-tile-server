package org.mapsforge.map.layer.renderer

import org.mapsforge.core.graphics.Bitmap
import org.mapsforge.core.graphics.Paint
import org.mapsforge.core.graphics.TileBitmap
import org.mapsforge.core.model.Point

public trait Shape : TileBitmap {
  public fun drawPolyLine(coordinates: Array<Array<Point>>, origin: Point, dy: Float)

  public fun beginFillOrSetLineStyle(paint: Paint): Boolean

  public fun endFill()

  public fun drawCircle(x: Double, y: Double, radius: Float)

  public fun drawTextRotated(text: String, start: Point, end: Point, origin: Point, paintFront: Paint)

  public fun drawSymbol(symbol: Bitmap, x: Double, y: Double, rotation: Float)
}

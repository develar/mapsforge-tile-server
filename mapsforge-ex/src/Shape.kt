package org.mapsforge.map.layer.renderer

import org.mapsforge.core.graphics.Bitmap
import org.mapsforge.core.graphics.Paint
import org.mapsforge.core.graphics.TileBitmap
import org.mapsforge.core.model.Point

public trait Shape : TileBitmap {
  public fun drawPolyLine(coordinates: Array<Array<Point>>, origin: Point, dy: Float): Unit

  public fun beginFillOrSetLineStyle(paint: Paint): Boolean

  public fun endFill(): Unit

  public fun drawCircle(x: Double, y: Double, radius: Float): Unit

  public fun drawTextRotated(text: String, start: Point, end: Point, origin: Point, paintFront: Paint): Unit

  public fun drawText(text: String, x: Double, y: Double, paintFront: Paint): Unit

  public fun drawSymbol(symbol: Bitmap, x: Double, y: Double, rotation: Float): Unit
}

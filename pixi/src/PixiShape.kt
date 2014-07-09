package org.develar.mapsforgeTileServer.pixi

import org.mapsforge.core.graphics.Bitmap
import org.mapsforge.core.graphics.Paint
import org.mapsforge.core.graphics.Style
import org.mapsforge.core.model.Point
import org.mapsforge.map.layer.renderer.Shape

import java.io.IOException
import java.io.OutputStream

class PixiShape(private val w: Int, private val h: Int) : DrawPath(), Shape {
  override fun getHeight(): Int  = w

  override fun getWidth(): Int = h

  throws(javaClass<IOException>())
  override fun compress(outputStream: OutputStream?) {
  }

  override fun incrementRefCount() {
  }

  override fun decrementRefCount() {
  }

  override fun scaleTo(width: Int, height: Int) {
  }

  override fun setBackgroundColor(color: Int) {
    throw UnsupportedOperationException()
  }

  override fun drawPolyLine(coordinates: Array<Array<Point>>, origin: Point, dy: Float) {
    writeCommand(PixiCommand.POLYLINE2)
    out.writeUnsighedVarInt(coordinates.size)

    var prevPoint = origin

    for (innerList in coordinates) {
      val points = innerList

      if (dy != 0f || points.size < 2) {
        throw IllegalStateException()
      }

      val moveTo = points[0]
      writeAsTwips(moveTo.x - prevPoint.x)
      writeAsTwips(moveTo.y - prevPoint.y)
      prevPoint = moveTo

      out.writeUnsighedVarInt(points.size - 1)

      for (i in 1..points.size - 1) {
        val point = points[i]
        writeAsTwips(point.x - prevPoint.x)
        writeAsTwips(point.y - prevPoint.y)
        prevPoint = point
      }
    }
  }

  override fun drawCircle(x: Double, y: Double, radius: Float) {
    writeCommand(PixiCommand.DRAW_CIRCLE2)
    writeAsTwips(x)
    writeAsTwips(y)
    writeAsTwips(radius)
  }

  override fun drawTextRotated(text: String, start: Point, end: Point, origin: Point, paintFront: Paint) {
    writeCommand(PixiCommand.ROTATED_TEXT)
    val x1 = start.x - origin.x
    val y1 = start.y - origin.y
    val x2 = end.x - origin.x
    val y2 = end.y - origin.y

    val dx = x2 - x1
    val dy = y2 - y1
    val rotation = Math.atan2(dy, dx)

    writeAsTwips(x1 + dx / 2)
    writeAsTwips(y1 + dy / 2)
    writeAsTwips(rotation)

    val textBounds = (paintFront as PixiPaint).getTextVisualBounds(text)

    writeAsTwips(textBounds.getWidth())
    writeAsTwips(textBounds.getHeight())

    out.writeString(text)
  }

  override fun drawSymbol(symbol: Bitmap, x: Double, y: Double, rotation: Float) {
    val pixiSymbol = symbol as PixiSymbol
    writeCommand(PixiCommand.SYMBOL)
    out.writeUnsighedVarInt(pixiSymbol.index)
    writeAsTwips(x)
    writeAsTwips(y)
    writeAsTwips(rotation)
  }

  override fun endFill() {
    writeCommand(PixiCommand.END_FILL)
  }

  override fun beginFillOrSetLineStyle(paint: Paint): Boolean {
    val pixiPaint = paint as PixiPaint
    if (pixiPaint._style == Style.FILL) {
      beginFill(pixiPaint._color)
      return true
    }
    else {
      if (pixiPaint.getAlpha() == 255) {
        writeCommand(PixiCommand.LINE_STYLE_RGB)
        writeAsTwips(pixiPaint.lineWidth)
        out.write((pixiPaint._color.ushr(16)) and 255)
        out.write((pixiPaint._color.ushr(8)) and 255)
        out.write((pixiPaint._color) and 255)
      }
      else {
        writeCommand(PixiCommand.LINE_STYLE_RGBA)
        writeAsTwips(pixiPaint.lineWidth)
        out.writeInt(pixiPaint._color)
      }
      return false
    }
  }
}

package org.develar.mapsforgeTileServer.pixi

import org.mapsforge.core.graphics.*
import org.mapsforge.core.model.Dimension
import org.mapsforge.map.layer.renderer.CanvasEx
import org.mapsforge.core.model.Point

private val TWIP_SIZE = 20

fun ByteArrayOutput.writeCommand(command:PixiCommand, color:Int) {
  write(command.ordinal())
  writeInt(color)
}

fun ByteArrayOutput.writeCommand(command:PixiCommand) {
  write(command.ordinal())
}

fun ByteArrayOutput.writePath(path:PixiPath) {
  path.closePolyline()
  path.out.writeTo(this)
}

// zig-zag encoding
// http://code.google.com/apis/protocolbuffers/docs/encoding.html#types
fun ByteArrayOutput.writeAsTwips(v:Float) {
  writeSignedVarInt(Math.round(v * TWIP_SIZE.toFloat()))
}

fun ByteArrayOutput.writeAsTwips(v:Double) {
  writeSignedVarInt(Math.round(v * TWIP_SIZE.toDouble()).toInt())
}

fun ByteArrayOutput.writeAsTwips(v:Int) {
  writeSignedVarInt(v * TWIP_SIZE)
}

fun ByteArrayOutput.beginFill(color:Int) {
  if (((color shr 24) and 255) == 255) {
    writeCommand(PixiCommand.BEGIN_FILL_RGB)
    write((color.ushr(16)) and 255)
    write((color.ushr(8)) and 255)
    write((color) and 255)
  }
  else {
    writeCommand(PixiCommand.BEGIN_FILL_RGBA, color)
  }
}

fun ByteArrayOutput.moveToOrLineTo(command:PixiCommand, x:Int, y:Int) {
  writeCommand(command)
  writeAsTwips(x)
  writeAsTwips(y)
}

fun ByteArrayOutput.drawCircle(command:PixiCommand, x:Int, y:Int, radius:Int) {
  write(command.ordinal())
  writeSignedVarInt(x)
  writeSignedVarInt(y)
  writeSignedVarInt(radius)
}

fun ByteArrayOutput.moveToOrLineTo(command:PixiCommand, x:Float, y:Float) {
  writeCommand(command)
  writeAsTwips(x)
  writeAsTwips(y)
}

class PixiCanvas() : Canvas, CanvasEx {
  val out = ByteArrayOutput()

  override fun setBitmap(bitmap:Bitmap?) = throw UnsupportedOperationException()

  override fun build():ByteArray = out.toByteArray()

  override fun reset() {
    out.reset()
  }

  override fun destroy() {
  }

  override fun getDimension():Dimension = Dimension(getWidth(), getHeight())

  override fun getHeight():Int = throw UnsupportedOperationException()

  override fun getWidth():Int = throw UnsupportedOperationException()

  override fun drawBitmap(bitmap:Bitmap, left:Int, top:Int) = throw IllegalStateException()

  override fun drawBitmap(bitmap:Bitmap, matrix:Matrix) {
  }

  override fun drawCircle(x:Int, y:Int, radius:Int, paint:Paint) {
    if (paint.isTransparent()) {
      return
    }

    val addEndFill = beginFillOrSetLineStyle(paint)
    out.drawCircle(PixiCommand.DRAW_CIRCLE, x, y, radius)
    if (addEndFill) {
      out.writeCommand(PixiCommand.END_FILL)
    }
  }

  override fun drawLine(x1:Int, y1:Int, x2:Int, y2:Int, paint:Paint) {
    if (paint.isTransparent()) {
      return
    }

    val addEndFill = beginFillOrSetLineStyle(paint)
    out.moveToOrLineTo(PixiCommand.MOVE_TO, x1, y1)
    out.moveToOrLineTo(PixiCommand.LINE_TO, x2, y2)
    if (addEndFill) {
      out.writeCommand(PixiCommand.END_FILL)
    }
  }

  override fun drawPath(path:Path, paint:Paint) {
    if (paint.isTransparent()) {
      return
    }

    val addEndFill = beginFillOrSetLineStyle(paint)
    out.writePath((path as PixiPath))
    if (addEndFill) {
      out.writeCommand(PixiCommand.END_FILL)
    }
  }

  override fun drawText(text:String, x:Float, y:Float, paint:Paint) = throw IllegalStateException("Shape.drawText must be used")

  override fun drawTextRotated(text:String, x1:Float, y1:Float, x2:Float, y2:Float, paint:Paint):Unit = throw IllegalStateException("Shape.drawTextRotated must be used")

  override fun fillColor(color:Color) {
  }

  override fun fillColor(color:Int) {
  }

  override fun resetClip() {
  }

  override fun setClip(left:Int, top:Int, width:Int, height:Int) {
  }

  override fun drawPolyLine(coordinates:Array<Array<Point>>, origin:Point, dy:Float) {
    out.writeCommand(PixiCommand.POLYLINE2)
    out.writeUnsighedVarInt(coordinates.size)

    var prevPoint = origin

    for (innerList in coordinates) {
      val points = innerList

      if (dy != 0f || points.size < 2) {
        throw IllegalStateException()
      }

      val moveTo = points[0]
      out.writeAsTwips(moveTo.x - prevPoint.x)
      out.writeAsTwips(moveTo.y - prevPoint.y)
      prevPoint = moveTo

      out.writeUnsighedVarInt(points.size - 1)

      for (i in 1..points.size - 1) {
        val point = points[i]
        out.writeAsTwips(point.x - prevPoint.x)
        out.writeAsTwips(point.y - prevPoint.y)
        prevPoint = point
      }
    }
  }

  override fun drawCircle(x:Double, y:Double, radius:Float) {
    out.writeCommand(PixiCommand.DRAW_CIRCLE2)
    out.writeAsTwips(x)
    out.writeAsTwips(y)
    out.writeAsTwips(radius)
  }

  override fun drawTextRotated(text:String, start:Point, end:Point, origin:Point, paintFront:Paint) {
    out.writeCommand(PixiCommand.ROTATED_TEXT)
    val x1 = start.x - origin.x
    val y1 = start.y - origin.y
    val x2 = end.x - origin.x
    val y2 = end.y - origin.y

    val dx = x2 - x1
    val dy = y2 - y1
    val rotation = Math.atan2(dy, dx)

    out.writeAsTwips(x1 + dx / 2)
    out.writeAsTwips(y1 + dy / 2)
    out.writeAsTwips(rotation)

    val textBounds = (paintFront as PixiPaint).getTextVisualBounds(text)

    out.writeAsTwips(textBounds.x)
    out.writeAsTwips(textBounds.y)

    out.writeString(text)
  }

  override fun drawText(text:String, x:Double, y:Double, paintFront:Paint) {
    out.writeCommand(PixiCommand.TEXT)
    out.writeAsTwips(x)
    out.writeAsTwips(y)
    out.writeString(text)
  }

  override fun drawSymbol(symbol:Bitmap, x:Double, y:Double, rotation:Float) {
    val pixiSymbol = symbol as PixiSymbol
    out.writeCommand(PixiCommand.SYMBOL)
    out.writeUnsighedVarInt(pixiSymbol.index)
    out.writeAsTwips(x)
    out.writeAsTwips(y)
    out.writeAsTwips(rotation)
  }

  override fun endFill() {
    out.writeCommand(PixiCommand.END_FILL)
  }

  override fun beginFillOrSetLineStyle(paint:Paint):Boolean {
    val pixiPaint = paint as PixiPaint
    if (pixiPaint._style == Style.FILL) {
      out.beginFill(pixiPaint._color)
      return true
    }
    else {
      if (pixiPaint.getAlpha() == 255) {
        out.writeCommand(PixiCommand.LINE_STYLE_RGB)
        out.writeAsTwips(pixiPaint.lineWidth)
        out.write((pixiPaint._color.ushr(16)) and 255)
        out.write((pixiPaint._color.ushr(8)) and 255)
        out.write((pixiPaint._color) and 255)
      }
      else {
        out.writeCommand(PixiCommand.LINE_STYLE_RGBA)
        out.writeAsTwips(pixiPaint.lineWidth)
        out.writeInt(pixiPaint._color)
      }
      return false
    }
  }
}
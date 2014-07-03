package org.develar.mapsforgeTileServer.pixi

import org.mapsforge.core.graphics.*
import org.mapsforge.core.model.Dimension

public class PixiCanvas() : Canvas {
  private var bitmap: PixiBitmap? = null

  override fun destroy() {
  }

  override fun getDimension(): Dimension = Dimension(getWidth(), getHeight())

  override fun getHeight(): Int = bitmap?.getHeight() ?: 0

  override fun getWidth(): Int = bitmap?.getWidth() ?: 0

  override fun setBitmap(bitmap: Bitmap?) {
    this.bitmap = bitmap as PixiBitmap
  }

  override fun drawBitmap(bitmap: Bitmap, left: Int, top: Int) {
    throw IllegalStateException()
  }

  override fun drawBitmap(bitmap: Bitmap, matrix: Matrix) {
  }

  override fun drawCircle(x: Int, y: Int, radius: Int, paint: Paint) {
    if (paint.isTransparent()) {
      return
    }

    val addEndFill = bitmap!!.beginFillOrSetLineStyle(paint)
    bitmap!!.drawCircle(PixiCommand.DRAW_CIRCLE, x, y, radius)
    if (addEndFill) {
      bitmap!!.writeCommand(PixiCommand.END_FILL)
    }
  }

  override fun drawLine(x1: Int, y1: Int, x2: Int, y2: Int, paint: Paint) {
    if (paint.isTransparent()) {
      return
    }

    val addEndFill = bitmap!!.beginFillOrSetLineStyle(paint)
    bitmap!!.moveToOrLineTo(PixiCommand.MOVE_TO, x1, y1)
    bitmap!!.moveToOrLineTo(PixiCommand.LINE_TO, x2, y2)
    if (addEndFill) {
      bitmap!!.writeCommand(PixiCommand.END_FILL)
    }
  }

  override fun drawPath(path: Path, paint: Paint) {
    if (paint.isTransparent()) {
      return
    }

    val addEndFill = bitmap!!.beginFillOrSetLineStyle(paint)
    bitmap!!.writePath((path as PixiPath))
    if (addEndFill) {
      bitmap!!.writeCommand(PixiCommand.END_FILL)
    }
  }

  override fun drawText(text: String, x: Float, y: Float, paint: Paint) {
    //System.out.println(x + " " + y);
    bitmap!!.writeCommand(PixiCommand.TEXT)
    bitmap!!.writeAsTwips(x)
    bitmap!!.writeAsTwips(y)
    bitmap!!.out.writeString(text)
  }

  override fun drawTextRotated(text: String, x1: Float, y1: Float, x2: Float, y2: Float, paint: Paint): Unit = throw IllegalStateException("Shape.drawTextRotated must be used")

  override fun fillColor(color: Color) {
  }

  override fun fillColor(color: Int) {
  }

  override fun resetClip() {
  }

  override fun setClip(left: Int, top: Int, width: Int, height: Int) {
  }
}
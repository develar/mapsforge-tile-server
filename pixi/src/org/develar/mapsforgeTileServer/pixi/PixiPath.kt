package org.develar.mapsforgeTileServer.pixi

import org.mapsforge.core.graphics.FillRule
import org.mapsforge.core.graphics.Path

public class PixiPath() : DrawPath(), Path {
  private var lineToCount = 0
  private var lineToCountOffset = -1

  private var prevX: Float = 0.toFloat()
  private var prevY: Float = 0.toFloat()

  override fun clear() = out.reset()

  override fun lineTo(x: Float, y: Float) {
    if (lineToCount == 0) {
      writeCommand(PixiCommand.POLYLINE)
      lineToCountOffset = out.allocateShort()

      prevX = 0f
      prevY = 0f
    }

    lineToCount++
    writeAsTwips(x - prevX)
    writeAsTwips(y - prevY)

    prevX = x
    prevY = y
  }

  override fun moveTo(x: Float, y: Float) {
    closePolyline()
    moveToOrLineTo(PixiCommand.MOVE_TO, x, y)
  }

  fun closePolyline() {
    if (lineToCount != 0) {
      assert(lineToCountOffset > 0)
      out.writeShort(lineToCount, lineToCountOffset)
      lineToCount = 0
      lineToCountOffset = -1
    }
  }

  override fun setFillRule(fillRule: FillRule): Unit = throw UnsupportedOperationException()
}
package org.develar.mapsforgeTileServer.pixi

import org.mapsforge.core.graphics.Canvas
import org.mapsforge.core.graphics.Matrix
import org.mapsforge.core.graphics.Paint
import org.mapsforge.core.graphics.Position
import org.mapsforge.core.mapelements.PointTextContainer
import org.mapsforge.core.mapelements.SymbolContainer
import org.mapsforge.core.model.Point
import org.mapsforge.core.model.Rectangle

public class PixiPointTextContainer(point: Point, priority: Int, text: String, paintFront: Paint, paintBack: Paint?, symbolContainer: SymbolContainer?, position: Position, maxTextWidth: Int) : PointTextContainer(point, priority, text, paintFront, paintBack, symbolContainer, position, maxTextWidth) {
  {
    boundary = computeBoundary()
  }

  override fun draw(canvas: Canvas, origin: Point, matrix: Matrix) {
  }

  private fun computeBoundary(): Rectangle? {
    val lines = textWidth / maxTextWidth + 1
    var boxWidth = textWidth.toDouble()
    var boxHeight = textHeight.toDouble()

    if (lines > 1) {
      // a crude approximation of the size of the text box
      boxWidth = maxTextWidth.toDouble()
      boxHeight = (textHeight * lines).toDouble()
    }

    when (position) {
      Position.CENTER -> {
        return Rectangle(-boxWidth / 2, -boxHeight / 2, boxWidth / 2, boxHeight / 2)
      }
      Position.BELOW -> {
        return Rectangle(-boxWidth / 2, 0.0, boxWidth / 2, boxHeight)
      }
      Position.ABOVE -> {
        return Rectangle(-boxWidth / 2, -boxHeight, boxWidth / 2, 0.0)
      }
      Position.LEFT -> {
        return Rectangle(-boxWidth, -boxHeight / 2, 0.0, boxHeight / 2)
      }
      Position.RIGHT -> {
        return Rectangle(0.0, -boxHeight / 2, boxWidth, boxHeight / 2)
      }
      else -> return null
    }
  }
}
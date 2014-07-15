package org.develar.mapsforgeTileServer.pixi

import org.mapsforge.core.graphics.Canvas
import org.mapsforge.core.graphics.Matrix
import org.mapsforge.core.graphics.Paint
import org.mapsforge.core.graphics.Position
import org.mapsforge.core.mapelements.SymbolContainer
import org.mapsforge.core.model.Point
import org.mapsforge.core.model.Rectangle
import org.mapsforge.map.layer.renderer.MapElementContainerEx
import org.mapsforge.map.layer.renderer.Shape
import org.mapsforge.core.mapelements.MapElementContainer
import java.awt.geom.Rectangle2D

class PixiPointTextContainer(private val text: String, point: Point, priority: Int, private val paintFront: Paint, paintBack: Paint?, symbolContainer: SymbolContainer?, position: Position, maxTextWidth: Int) : MapElementContainer(point, priority), MapElementContainerEx {
  {
    val textVisualBounds: Rectangle2D
    if (paintBack != null) {
      textVisualBounds = (paintBack as PixiPaint).getTextVisualBounds(text)
    }
    else {
      textVisualBounds = (paintFront as PixiPaint).getTextVisualBounds(text);
    }
    boundary = computeBoundary(textVisualBounds, maxTextWidth, position)
  }

  override fun draw(shape: Shape, origin: Point) {
    if (text.startsWith("21")) {
      System.out.print("dd");
    }
    shape.drawText(text, (xy.x - origin.x) + boundary.left, (xy.y - origin.y) + boundary.top, paintFront)
  }

  override fun clashesWith(other: MapElementContainer): Boolean {
    if (super<MapElementContainer>.clashesWith(other)) {
      return true;
    }
    return other is PixiPointTextContainer && text == other.text && getPoint().distance(other.getPoint()) < 200
  }

  override fun draw(canvas: Canvas, origin: Point, matrix: Matrix) = throw IllegalStateException()

  override fun equals(other: Any?): Boolean {
    if (!super<MapElementContainer>.equals(other)) {
      return false;
    }
    return other is PixiPointTextContainer && text == other.text;
  }

  override fun hashCode(): Int {
    return 31 * super<MapElementContainer>.hashCode() + text.hashCode();
  }

  private fun computeBoundary(textVisualBounds: Rectangle2D, maxTextWidth: Int, position: Position): Rectangle {
    val lines = textVisualBounds.getWidth() / maxTextWidth + 1
    var boxWidth = textVisualBounds.getWidth()
    var boxHeight = textVisualBounds.getHeight()

    if (lines > 1) {
      // a crude approximation of the size of the text box
      //boxWidth = maxTextWidth.toDouble()
      //boxHeight = (textHeight * lines).toDouble()
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
      else -> throw UnsupportedOperationException()
    }
  }
}
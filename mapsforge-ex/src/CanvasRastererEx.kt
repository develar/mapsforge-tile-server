package org.mapsforge.map.layer.renderer

import org.mapsforge.core.graphics.Paint
import org.mapsforge.core.mapelements.MapElementContainer
import org.mapsforge.core.mapelements.SymbolContainer
import org.mapsforge.core.mapelements.WayTextContainer
import org.mapsforge.core.model.Point
import org.mapsforge.core.model.Tile

val EMPTY_POINT:Point = Point(0.0, 0.0)

public trait MapElementContainerEx {
  fun draw(canvas:CanvasEx, origin:Point):Unit
}

fun drawWays(drawWays:Array<List<MutableList<ShapePaintContainer>>>, canvas:CanvasEx) {
  val levelsPerLayer = drawWays[0].size()
  var currentStroke:Paint? = null
  for (shapePaintContainers in drawWays) {
    for (level in 0..levelsPerLayer - 1) {
      val wayList = shapePaintContainers.get(level)
      for (i in wayList.size() - 1 downTo 0) {
        val container = wayList.get(i)

        assert(container.paint == null)
        val fill = container.fill
        var addEndFill = false
        if (fill != null && !fill.isTransparent()) {
          addEndFill = canvas.beginFillOrSetLineStyle(fill)
          assert(addEndFill)
        }

        if (currentStroke != container.stroke) {
          currentStroke = container.stroke
          if (currentStroke != null && !currentStroke!!.isTransparent()) {
            val r = canvas.beginFillOrSetLineStyle(currentStroke!!)
            assert(!r)
          }
        }

        drawShapePaintContainer(container, canvas)

        if (addEndFill) {
          canvas.endFill()
        }
      }
    }
  }
}

private fun drawShapePaintContainer(shapePaintContainer:ShapePaintContainer, canvas:CanvasEx) {
  when (shapePaintContainer.shapeContainer.getShapeType()) {
    ShapeType.CIRCLE -> {
      val circleContainer = shapePaintContainer.shapeContainer as CircleContainer
      val point = circleContainer.point
      canvas.drawCircle(point.x, point.y, circleContainer.radius)
    }

    ShapeType.POLYLINE -> {
      val shapeContainer = shapePaintContainer.shapeContainer as PolylineContainer
      val coordinates = if (shapePaintContainer.dy == 0f) shapeContainer.getCoordinatesAbsolute() else shapeContainer.getCoordinatesRelativeToTile()
      [suppress("CAST_NEVER_SUCCEEDS")]
      canvas.drawPolyLine(coordinates, if (shapePaintContainer.dy == 0f) shapeContainer.getTile().getOrigin() else EMPTY_POINT, shapePaintContainer.dy)
    }
    else -> {
    }
  }
}

fun drawMapElements(elements:Collection<MapElementContainer>, tile:Tile, canvas:CanvasEx) {
  val origin = tile.getOrigin()
  for (element in elements) {
    if (element is WayTextContainer) {
      canvas.drawTextRotated(element.text, element.getPoint(), element.end, origin, element.paintFront)
    }
    else if (element is SymbolContainer) {
      val boundary = element.getBoundary()
      val point = element.getPoint()
      if (element.theta != 0f) {
        throw UnsupportedOperationException("rotated symbol not supported")
      }
      canvas.drawSymbol(element.symbol, (point.x - origin.x) + boundary.left, (point.y - origin.y) + boundary.top, element.theta)
    }
    else if (element is MapElementContainerEx) {
      element.draw(canvas, origin);
    }
    else {
      throw UnsupportedOperationException("unsupported element")
    }
  }
}
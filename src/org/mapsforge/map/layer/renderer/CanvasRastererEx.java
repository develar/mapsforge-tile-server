package org.mapsforge.map.layer.renderer;

import org.jetbrains.annotations.NotNull;
import org.mapsforge.core.graphics.*;
import org.mapsforge.core.mapelements.MapElementContainer;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Tile;

import java.util.Collection;
import java.util.List;

public class CanvasRastererEx {
  private final Canvas canvas;
  private final Matrix symbolMatrix;

  CanvasRastererEx(@NotNull GraphicFactory graphicFactory) {
    canvas = graphicFactory.createCanvas();
    symbolMatrix = graphicFactory.createMatrix();
  }

  static void drawWays(List<List<ShapePaintContainer>>[] drawWays, Shape shape) {
    int levelsPerLayer = drawWays[0].size();
    Paint currentStroke = null;
    for (List<List<ShapePaintContainer>> shapePaintContainers : drawWays) {
      for (int level = 0; level < levelsPerLayer; level++) {
        List<ShapePaintContainer> wayList = shapePaintContainers.get(level);
        for (int i = wayList.size() - 1; i >= 0; i--) {
          ShapePaintContainer container = wayList.get(i);

          assert container.paint == null;
          Paint fill = container.fill;
          boolean addEndFill = false;
          if (fill != null && !fill.isTransparent()) {
            addEndFill = shape.beginFillOrSetLineStyle(fill);
            assert addEndFill;
          }

          if (currentStroke != container.stroke) {
            currentStroke = container.stroke;
            if (currentStroke != null && !currentStroke.isTransparent()) {
              boolean r = shape.beginFillOrSetLineStyle(currentStroke);
              assert !r;
            }
          }

          drawShapePaintContainer(container, shape);

          if (addEndFill) {
            shape.endFill();
          }
        }
      }
    }
  }

  void drawMapElements(Collection<MapElementContainer> elements, Tile tile) {
    Point origin = tile.getOrigin();
    for (MapElementContainer element : elements) {
      element.draw(canvas, origin, symbolMatrix);
    }
  }

  void fill(int color) {
    if (GraphicUtils.getAlpha(color) > 0) {
      canvas.fillColor(color);
    }
  }

  void setCanvasBitmap(Bitmap bitmap) {
    canvas.setBitmap(bitmap);
  }

  private static void drawCircleContainer(@NotNull ShapePaintContainer shapePaintContainer, @NotNull Shape shape) {
    CircleContainer circleContainer = (CircleContainer)shapePaintContainer.shapeContainer;
    Point point = circleContainer.point;
    shape.drawCircle(point.x, point.y, circleContainer.radius);
  }

  private static void drawPath(@NotNull ShapePaintContainer shapePaintContainer, float dy, @NotNull Shape shape) {
    PolylineContainer shapeContainer = (PolylineContainer)shapePaintContainer.shapeContainer;
    Point[][] coordinates = dy == 0 ? shapeContainer.getCoordinatesAbsolute() : shapeContainer.getCoordinatesRelativeToTile();
    Point tileOrigin = dy == 0 ? shapeContainer.getTile().getOrigin() : new Point(0, 0);
    for (Point[] innerList : coordinates) {
      Point[] points = dy == 0 ? innerList : RendererUtils.parallelPath(innerList, dy);
      if (points.length >= 2) {
        shape.drawPolyLine(points, tileOrigin);
      }
    }
  }

  private static void drawShapePaintContainer(@NotNull ShapePaintContainer shapePaintContainer, @NotNull Shape shape) {
    switch (shapePaintContainer.shapeContainer.getShapeType()) {
      case CIRCLE:
        drawCircleContainer(shapePaintContainer, shape);
        break;

      case POLYLINE:
        drawPath(shapePaintContainer, shapePaintContainer.dy, shape);
        break;
    }
  }
}
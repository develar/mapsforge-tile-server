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
    for (List<List<ShapePaintContainer>> shapePaintContainers : drawWays) {
      for (int level = 0; level < levelsPerLayer; level++) {
        List<ShapePaintContainer> wayList = shapePaintContainers.get(level);
        for (int index = wayList.size() - 1; index >= 0; --index) {
          drawShapePaintContainer(wayList.get(index), shape);
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

  private static int beginFillOrSetLineStyle(@NotNull ShapePaintContainer shapePaintContainer, @NotNull Shape shape) {
    if (shapePaintContainer.paint == null) {
      int addEndFill = shapePaintContainer.fill != null && !shapePaintContainer.fill.isTransparent() && shape.beginFillOrSetLineStyle(shapePaintContainer.fill) ? 1 : 0;
      Paint stroke = shapePaintContainer.stroke;
      if (stroke != null && !stroke.isTransparent()) {
        shape.beginFillOrSetLineStyle(stroke);
      }
      else if (addEndFill == 0) {
        return - 1;
      }
      return addEndFill;
    }
    else {
      if (shapePaintContainer.paint.isTransparent()) {
        return -1;
      }
      else {
        return shape.beginFillOrSetLineStyle(shapePaintContainer.paint) ? 1 : 0;
      }
    }
  }

  private static void drawCircleContainer(@NotNull ShapePaintContainer shapePaintContainer, @NotNull Shape shape) {
    CircleContainer circleContainer = (CircleContainer)shapePaintContainer.shapeContainer;
    Point point = circleContainer.point;
    shape.drawCircle(point.x, point.y, circleContainer.radius);
  }

  private static void drawPath(@NotNull ShapePaintContainer shapePaintContainer, float dy, @NotNull Shape shape) {
    PolylineContainer shapeContainer = (PolylineContainer)shapePaintContainer.shapeContainer;
    Point[][] coordinates = dy == 0 ? shapeContainer.getCoordinatesAbsolute() : shapeContainer.getCoordinatesRelativeToTile();
    Point tileOrigin = dy == 0 ? shapeContainer.getTile().getOrigin() : null;
    for (Point[] innerList : coordinates) {
      Point[] points = dy == 0 ? innerList : RendererUtils.parallelPath(innerList, dy);
      if (points.length >= 2) {
        shape.drawPolyLine(points, tileOrigin);
      }
    }
  }

  private static void drawShapePaintContainer(@NotNull ShapePaintContainer shapePaintContainer, @NotNull Shape shape) {
    int addEndFill = beginFillOrSetLineStyle(shapePaintContainer, shape);
    if (addEndFill == -1) {
      return;
    }

    switch (shapePaintContainer.shapeContainer.getShapeType()) {
      case CIRCLE:
        drawCircleContainer(shapePaintContainer, shape);
        break;

      case POLYLINE:
        drawPath(shapePaintContainer, shapePaintContainer.dy, shape);
        break;
    }

    if (addEndFill == 1) {
      shape.endFill();
    }
  }
}
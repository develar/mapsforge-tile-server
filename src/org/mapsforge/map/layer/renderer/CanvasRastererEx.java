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

  void drawWays(List<List<ShapePaintContainer>>[] drawWays, Shape shape) {
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

  private void drawCircleContainer(ShapePaintContainer shapePaintContainer) {
    CircleContainer circleContainer = (CircleContainer)shapePaintContainer.shapeContainer;
    Point point = circleContainer.point;
    canvas.drawCircle((int)point.x, (int)point.y, (int)circleContainer.radius, shapePaintContainer.paint);
  }

  private static void drawPath(ShapePaintContainer shapePaintContainer, float dy, Shape shape) {
    if (shapePaintContainer.paint.isTransparent()) {
      return;
    }

    boolean addEndFill = shape.beginFillOrSetLineStyle(shapePaintContainer.paint);

    PolylineContainer shapeContainer = (PolylineContainer)shapePaintContainer.shapeContainer;
    Point[][] coordinates = dy == 0 ? shapeContainer.getCoordinatesAbsolute() : shapeContainer.getCoordinatesRelativeToTile();
    Point tileOrigin = dy == 0 ? shapeContainer.getTile().getOrigin() : null;
    for (Point[] innerList : coordinates) {
      Point[] points = dy == 0 ? innerList : RendererUtils.parallelPath(innerList, dy);
      if (points.length >= 2) {
        shape.drawPolyLine(points, tileOrigin);
      }
    }

    if (addEndFill) {
      shape.endFill();
    }
  }

  private void drawShapePaintContainer(ShapePaintContainer shapePaintContainer, Shape shape) {
    switch (shapePaintContainer.shapeContainer.getShapeType()) {
      case CIRCLE:
        drawCircleContainer(shapePaintContainer);
        break;

      case POLYLINE:
        drawPath(shapePaintContainer, shapePaintContainer.dy, shape);
        break;
    }
  }
}
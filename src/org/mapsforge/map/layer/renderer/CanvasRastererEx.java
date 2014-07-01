package org.mapsforge.map.layer.renderer;

import org.jetbrains.annotations.NotNull;
import org.mapsforge.core.graphics.*;
import org.mapsforge.core.mapelements.MapElementContainer;
import org.mapsforge.core.mapelements.WayTextContainer;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Tile;

import java.util.Collection;
import java.util.List;

public class CanvasRastererEx {
  public static final Point EMPTY_POINT = new Point(0, 0);

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

  void drawMapElements(Collection<MapElementContainer> elements, Tile tile, Shape shape) {
    Point origin = tile.getOrigin();
    for (MapElementContainer element : elements) {
      if (element instanceof WayTextContainer) {
        WayTextContainer wayTextContainer = (WayTextContainer)element;
        shape.drawTextRotated(wayTextContainer.text, wayTextContainer.getPoint(), wayTextContainer.end, origin, wayTextContainer.paintFront);
      }
      else {
        element.draw(canvas, origin, symbolMatrix);
      }
    }
  }

  void setCanvasBitmap(Bitmap bitmap) {
    canvas.setBitmap(bitmap);
  }

  private static void drawShapePaintContainer(@NotNull ShapePaintContainer shapePaintContainer, @NotNull Shape shape) {
    switch (shapePaintContainer.shapeContainer.getShapeType()) {
      case CIRCLE:
        CircleContainer circleContainer = (CircleContainer)shapePaintContainer.shapeContainer;
        Point point = circleContainer.point;
        shape.drawCircle(point.x, point.y, circleContainer.radius);
        break;

      case POLYLINE:
        PolylineContainer shapeContainer = (PolylineContainer)shapePaintContainer.shapeContainer;
        Point[][] coordinates = shapePaintContainer.dy == 0 ? shapeContainer.getCoordinatesAbsolute() : shapeContainer.getCoordinatesRelativeToTile();
        shape.drawPolyLine(coordinates, shapePaintContainer.dy == 0 ? shapeContainer.getTile().getOrigin() : EMPTY_POINT, shapePaintContainer.dy);
        break;
    }
  }
}
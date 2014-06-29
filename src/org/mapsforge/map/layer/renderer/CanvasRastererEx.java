package org.mapsforge.map.layer.renderer;

import org.mapsforge.core.graphics.*;
import org.mapsforge.core.mapelements.MapElementContainer;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Tile;

import java.util.Collection;
import java.util.List;

public class CanvasRastererEx {
  private final Canvas canvas;
  	private final Path path;
  	private final Matrix symbolMatrix;

  	CanvasRastererEx(GraphicFactory graphicFactory) {
      canvas = graphicFactory.createCanvas();
      symbolMatrix = graphicFactory.createMatrix();
      path = graphicFactory.createPath();
  	}

  	void drawWays(List<List<List<ShapePaintContainer>>> drawWays) {
  		int levelsPerLayer = drawWays.get(0).size();

  		for (List<List<ShapePaintContainer>> shapePaintContainers : drawWays) {
  			for (int level = 0; level < levelsPerLayer; ++level) {
  				List<ShapePaintContainer> wayList = shapePaintContainers.get(level);

  				for (int index = wayList.size() - 1; index >= 0; --index) {
  					drawShapePaintContainer(wayList.get(index));
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
  		CircleContainer circleContainer = (CircleContainer) shapePaintContainer.shapeContainer;
  		Point point = circleContainer.point;
      canvas.drawCircle((int)point.x, (int)point.y, (int)circleContainer.radius, shapePaintContainer.paint);
  	}

  	private void drawPath(ShapePaintContainer shapePaintContainer, Point[][] coordinates, float dy) {
      path.clear();

  		for (Point[] innerList : coordinates) {
  			Point[] points;
  			if (dy != 0f) {
  				points = RendererUtils.parallelPath(innerList, dy);
  			} else {
  				points = innerList;
  			}
  			if (points.length >= 2) {
  				Point point = points[0];
          path.moveTo((float)point.x, (float)point.y);
  				for (int i = 1; i < points.length; ++i) {
  					point = points[i];
            path.lineTo((int)point.x, (int)point.y);
  				}
  			}
  		}

      canvas.drawPath(path, shapePaintContainer.paint);
  	}

  	private void drawShapePaintContainer(ShapePaintContainer shapePaintContainer) {
      switch (shapePaintContainer.shapeContainer.getShapeType()) {
  			case CIRCLE:
  				drawCircleContainer(shapePaintContainer);
  				break;

  			case POLYLINE:
  				PolylineContainer polylineContainer = (PolylineContainer) shapePaintContainer.shapeContainer;
  				drawPath(shapePaintContainer, polylineContainer.getCoordinatesRelativeToTile(), shapePaintContainer.dy);
          break;
      }
  	}
}

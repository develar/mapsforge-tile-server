package org.develar.mapsforgeTileServer.pixi;

import org.jetbrains.annotations.NotNull;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.core.model.Point;
import org.mapsforge.map.layer.renderer.Shape;

import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.OutputStream;

public class PixiBitmap extends DrawPath implements Shape {
  private final int width;
  private final int height;

  public PixiBitmap(int width, int height) {
    this.width = width;
    this.height = height;
  }

  @Override
  public void compress(OutputStream outputStream) throws IOException {
  }

  @Override
  public int getHeight() {
    return height;
  }

  @Override
  public int getWidth() {
    return width;
  }

  @Override
  public void incrementRefCount() {
  }

  @Override
  public void decrementRefCount() {
  }

  @Override
  public void scaleTo(int width, int height) {
  }

  @Override
  public void setBackgroundColor(int color) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void drawPolyLine(@NotNull Point[][] coordinates, @NotNull Point origin, float dy) {
    writeCommand(PixiCommand.POLYLINE2);
    out.writeUnsighedVarInt(coordinates.length);

    Point prevPoint = origin;

    for (Point[] innerList : coordinates) {
      //Point[] points = dy == 0 ? innerList : RendererUtils.parallelPath(innerList, dy);
      @SuppressWarnings("UnnecessaryLocalVariable")
      Point[] points = innerList;

      if (dy != 0 || points.length < 2) {
        throw new IllegalStateException();
      }

      Point moveTo = points[0];
      writeAsTwips(moveTo.x - prevPoint.x);
      writeAsTwips(moveTo.y - prevPoint.y);
      prevPoint = moveTo;

      out.writeUnsighedVarInt(points.length - 1);

      for (int i = 1; i < points.length; i++) {
        Point point = points[i];
        writeAsTwips(point.x - prevPoint.x);
        writeAsTwips(point.y - prevPoint.y);
        prevPoint = point;
      }
    }
  }

  @Override
  public void drawCircle(double x, double y, float radius) {
    writeCommand(PixiCommand.DRAW_CIRCLE2);
    writeAsTwips(x);
    writeAsTwips(y);
    writeAsTwips(radius);
  }

  @Override
  public void drawTextRotated(@NotNull String text, @NotNull Point start, @NotNull Point end, @NotNull Point origin, Paint paintFront) {
    writeCommand(PixiCommand.ROTATED_TEXT);
    double x1 = start.x - origin.x;
    double y1 = start.y - origin.y;
    double x2 = end.x - origin.x;
    double y2 = end.y - origin.y;

    double lineLength = Math.hypot(x2 - x1, y2 - y1);
    Rectangle2D textBounds = ((PixiPaint)paintFront).getTextVisualBounds(text);
    double dx = (lineLength - textBounds.getWidth()) / 2;
    double dy = textBounds.getHeight() / 3;

    double theta = Math.atan2(y2 - y1, x2 - x1);

    writeAsTwips(x1 + dx);
    writeAsTwips(y1 + dy);

    writeAsTwips(theta);

    writeAsTwips(x1);
    writeAsTwips(y1);

    out.writeString(text);
  }

  @Override
  public void endFill() {
    writeCommand(PixiCommand.END_FILL);
  }

  public final boolean beginFillOrSetLineStyle(@NotNull Paint paint) {
    PixiPaint pixiPaint = (PixiPaint)paint;
    if (pixiPaint.style == Style.FILL) {
      beginFill(pixiPaint.color);
      return true;
    }
    else {
      if (pixiPaint.getAlpha() == 255) {
        writeCommand(PixiCommand.LINE_STYLE_RGB);
        writeAsTwips(pixiPaint.lineWidth);
        out.write((pixiPaint.color >>> 16) & 0xFF);
        out.write((pixiPaint.color >>> 8) & 0xFF);
        out.write((pixiPaint.color) & 0xFF);
      }
      else {
        writeCommand(PixiCommand.LINE_STYLE_RGBA);
        writeAsTwips(pixiPaint.lineWidth);
        out.writeInt(pixiPaint.color);
      }
      return false;
    }
  }
}

package org.develar.mapsforgeTileServer.pixi;

import org.jetbrains.annotations.NotNull;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.core.model.Point;
import org.mapsforge.map.layer.renderer.Shape;

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
  public void drawPolyLine(@NotNull Point[] points, @NotNull Point origin) {
    writeCommand(PixiCommand.POLYLINE2);

    Point moveTo = points[0];
    writeAsTwips(moveTo.x - origin.x);
    writeAsTwips(moveTo.y - origin.y);
    Point prevPoint = moveTo;

    out.writeUnsighedVarInt(points.length - 1);

    for (int i = 1; i < points.length; i++) {
      Point point = points[i];
      writeAsTwips(point.x - prevPoint.x);
      writeAsTwips(point.y - prevPoint.y);
      prevPoint = point;
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

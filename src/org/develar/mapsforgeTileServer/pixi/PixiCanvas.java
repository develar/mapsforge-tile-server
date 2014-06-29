package org.develar.mapsforgeTileServer.pixi;

import org.mapsforge.core.graphics.*;
import org.mapsforge.core.model.Dimension;

public class PixiCanvas implements Canvas {
  private PixiBitmap bitmap;

  @Override
  public void destroy() {
  }

  @Override
  public Dimension getDimension() {
    return new Dimension(getWidth(), getHeight());
  }

  @Override
  public int getHeight() {
    return bitmap == null ? 0 : bitmap.getHeight();
  }

  @Override
  public int getWidth() {
    return bitmap == null ? 0 : bitmap.getWidth();
  }

  @Override
  public void setBitmap(Bitmap bitmap) {
    this.bitmap = (PixiBitmap)bitmap;
  }

  @Override
  public void drawBitmap(Bitmap bitmap, int left, int top) {
  }

  @Override
  public void drawBitmap(Bitmap bitmap, Matrix matrix) {
  }

  @Override
  public void drawCircle(int x, int y, int radius, Paint paint) {
    if (paint.isTransparent()) {
      return;
    }

    boolean addEndFill = beginFillOrSetLineStyle((PixiPaint)paint);
    bitmap.drawCircle(PixiCommand.DRAW_CIRCLE, x, y, radius);
    if (addEndFill) {
      bitmap.writeCommand(PixiCommand.END_FILL);
    }
  }

  @Override
  public void drawLine(int x1, int y1, int x2, int y2, Paint paint) {
    if (paint.isTransparent()) {
      return;
    }

    boolean addEndFill = beginFillOrSetLineStyle((PixiPaint)paint);
    bitmap.moveToOrLineTo(PixiCommand.MOVE_TO, x1, y1);
    bitmap.moveToOrLineTo(PixiCommand.LINE_TO, x2, y2);
    if (addEndFill) {
      bitmap.writeCommand(PixiCommand.END_FILL);
    }
  }

  @Override
  public void drawPath(Path path, Paint paint) {
    if (paint.isTransparent()) {
      return;
    }

    PixiPaint pixiPaint = (PixiPaint)paint;
    boolean addEndFill = beginFillOrSetLineStyle(pixiPaint);
    bitmap.writePath(((PixiPath)path));
    if (addEndFill) {
      bitmap.writeCommand(PixiCommand.END_FILL);
    }
  }

  private boolean beginFillOrSetLineStyle(PixiPaint pixiPaint) {
    if (pixiPaint.style == Style.FILL) {
      bitmap.beginFill(pixiPaint.color);
      return true;
    }
    else {
      if (pixiPaint.getAlpha() == 255) {
        bitmap.writeCommand(PixiCommand.LINE_STYLE_RGB);
        bitmap.writeAsTwips(pixiPaint.lineWidth);
        bitmap.out.write((pixiPaint.color >>> 16) & 0xFF);
        bitmap.out.write((pixiPaint.color >>> 8) & 0xFF);
        bitmap.out.write((pixiPaint.color) & 0xFF);
      }
      else {
        bitmap.writeCommand(PixiCommand.LINE_STYLE_RGBA);
        bitmap.writeAsTwips(pixiPaint.lineWidth);
        bitmap.out.writeInt(pixiPaint.color);
      }
      return false;
    }
  }

  @Override
  public void drawText(String text, float x, float y, Paint paint) {
    //System.out.println(x + " " + y);
    bitmap.writeCommand(PixiCommand.TEXT);
    bitmap.writeAsTwips(x);
    bitmap.writeAsTwips(y);
    bitmap.out.writeString(text);
  }

  @Override
  public void drawTextRotated(String text, float x1, float y1, float x2, float y2, Paint paint) {
    //System.out.println(x1 + " " + y1);
    bitmap.writeCommand(PixiCommand.ROTATED_TEXT);
    bitmap.writeAsTwips(x1);
    bitmap.writeAsTwips(y1);
    bitmap.writeAsTwips(x2 - x1);
    bitmap.writeAsTwips(y2 - y1);
    bitmap.out.writeString(text);
  }

  @Override
  public void fillColor(Color color) {
  }

  @Override
  public void fillColor(int color) {
  }

  @Override
  public void resetClip() {
  }

  @Override
  public void setClip(int left, int top, int width, int height) {
  }
}
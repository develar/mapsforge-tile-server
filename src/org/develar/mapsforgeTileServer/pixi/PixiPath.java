package org.develar.mapsforgeTileServer.pixi;

import org.mapsforge.core.graphics.FillRule;
import org.mapsforge.core.graphics.Path;

public class PixiPath extends DrawPath implements Path {
  private int lineToCount = 0;
  private int lineToCountOffset = -1;

  @Override
  public void clear() {
    out.reset();
  }

  @Override
  public void lineTo(float x, float y) {
    if (lineToCount == 0) {
      writeCommand(PixiCommand.POLYLINE);
      lineToCountOffset = out.allocateShort();
    }

    lineToCount++;
    writeAsTwips(x);
    writeAsTwips(y);
  }

  @Override
  public void moveTo(float x, float y) {
    closePolyline();
    moveToOrLineTo(PixiCommand.MOVE_TO, x, y);
  }

  void closePolyline() {
    if (lineToCount != 0) {
      assert lineToCountOffset > 0;
      out.writeShort(lineToCount, lineToCountOffset);
      lineToCount = 0;
      lineToCountOffset = -1;
    }
  }

  @Override
  public void setFillRule(FillRule fillRule) {
    throw new UnsupportedOperationException();
  }
}
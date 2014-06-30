package org.mapsforge.map.layer.renderer;

import org.jetbrains.annotations.NotNull;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.TileBitmap;
import org.mapsforge.core.model.Point;

public interface Shape extends TileBitmap {
  void drawPolyLine(@NotNull Point[] points, @NotNull Point origin);

  boolean beginFillOrSetLineStyle(@NotNull Paint paint);

  void endFill();

  void drawCircle(double x, double y, float radius);
}

package org.mapsforge.map.layer.renderer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.TileBitmap;
import org.mapsforge.core.model.Point;

public interface Shape extends TileBitmap {
  void drawPolyLine(@NotNull Point[] points, @Nullable Point tileOrigin);

  boolean beginFillOrSetLineStyle(Paint paint);

  void endFill();
}
package org.mapsforge.map.layer.renderer;

import org.jetbrains.annotations.NotNull;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.TileBitmap;
import org.mapsforge.core.model.Point;

public interface Shape extends TileBitmap {
  void drawPolyLine(@NotNull Point[][] coordinates, @NotNull Point origin, float dy);

  boolean beginFillOrSetLineStyle(@NotNull Paint paint);

  void endFill();

  void drawCircle(double x, double y, float radius);

  void drawTextRotated(@NotNull String text, @NotNull Point start, @NotNull Point end, @NotNull Point origin, Paint paintBack);
}

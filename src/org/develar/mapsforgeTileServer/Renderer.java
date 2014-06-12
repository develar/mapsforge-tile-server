package org.develar.mapsforgeTileServer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Tile;

import java.io.File;
import java.util.List;

public final class Renderer {
  private TileRenderer[] tileRenderers;

  public final StringBuilder stringBuilder = new StringBuilder();

  @Nullable
  public TileRenderer getTileRenderer(@NotNull Tile tile, @NotNull MapsforgeTileServer tileServer) {
    if (tileRenderers == null) {
      tileRenderers = new TileRenderer[tileServer.maps.size()];
    }

    int y = (int)tile.tileY;
    double north = tileToLat(y, tile.zoomLevel);
    double south = tileToLat(y + 1, tile.zoomLevel);
    int x = (int)tile.tileX;
    double west = tileToLon(x, tile.zoomLevel);
    double east = tileToLon(x + 1, tile.zoomLevel);

    List<File> maps = tileServer.maps;
    //noinspection LoopStatementThatDoesntLoop
    for (int i = 0, n = maps.size(); i < n; i++) {
      File mapFile = maps.get(i);
      TileRenderer tileRenderer = tileRenderers[i];
      if (tileRenderer == null) {
        tileRenderer = new TileRenderer(tileServer.displayModel, mapFile, tileServer.defaultRenderTheme);
        tileRenderers[i] = tileRenderer;
      }

      BoundingBox mapBoundingBox = tileRenderer.getBoundingBox();
      if (intersects(west, east, south, north, mapBoundingBox)) {
        return tileRenderer;
      }
    }

    return null;
  }

  @SuppressWarnings("UnnecessaryLocalVariable")
  public static boolean intersects(double minLongitude, double maxLongitude, double minLatitude, double maxLatitude, @NotNull BoundingBox mapBoundingBox) {
    double tw = maxLongitude - minLongitude;
    double th = maxLatitude - minLatitude;
    double rw = mapBoundingBox.getLongitudeSpan();
    double rh = mapBoundingBox.getLatitudeSpan();
    if (rw <= 0 || rh <= 0 || tw <= 0 || th <= 0) {
      return false;
    }

    double tx = minLongitude;
    double ty = minLatitude;
    double rx = mapBoundingBox.minLongitude;
    double ry = mapBoundingBox.minLatitude;
    rw += rx;
    rh += ry;
    tw += tx;
    th += ty;
    // overflow || intersect
    return ((rw < rx || rw > minLongitude) &&
              (rh < ry || rh > minLatitude) &&
              (tw < minLongitude || tw > rx) &&
              (th < minLatitude || th > ry));
  }

  static double tileToLon(int x, byte z) {
    return x / Math.pow(2.0, z) * 360.0 - 180;
  }

  static double tileToLat(int y, byte z) {
    double n = Math.PI - (2.0 * Math.PI * y) / Math.pow(2.0, z);
    return Math.toDegrees(Math.atan(Math.sinh(n)));
  }
}
package org.develar.mapsforgeTileServer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Tile;
import org.mapsforge.core.util.MercatorProjection;

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

    double longitude = MercatorProjection.tileXToLongitude(tile.tileX, tile.zoomLevel);
    double latitude = MercatorProjection.tileYToLatitude(tile.tileY, tile.zoomLevel);

    List<File> maps = tileServer.maps;
    //noinspection LoopStatementThatDoesntLoop
    for (int i = 0, n = maps.size(); i < n; i++) {
      File mapFile = maps.get(i);
      TileRenderer tileRenderer = tileRenderers[i];
      if (tileRenderer == null) {
        tileRenderer = new TileRenderer(tileServer.displayModel, mapFile, tileServer.defaultRenderTheme);
        tileRenderers[i] = tileRenderer;
      }

      BoundingBox boundingBox = tileRenderer.getBoundingBox();
      if (boundingBox.minLatitude <= latitude && boundingBox.maxLatitude >= latitude &&
        boundingBox.minLongitude <= longitude && boundingBox.maxLongitude >= longitude) {
        return tileRenderer;
      }
    }

    return null;
  }
}

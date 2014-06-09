package org.develar.mapsforgeTileServer;

import org.jetbrains.annotations.NotNull;
import org.mapsforge.core.model.Tile;

import java.io.File;

public final class Renderer {
  private TileRenderer[] tileRenderers;

  public final StringBuilder stringBuilder = new StringBuilder();

  @NotNull
  public TileRenderer getTileRenderer(@SuppressWarnings("UnusedParameters") @NotNull Tile tile, @NotNull MapsforgeTileServer tileServer) {
    if (tileRenderers == null) {
      tileRenderers = new TileRenderer[tileServer.maps.length];
    }
    return getTileRenderer(tileServer);
  }

  @NotNull
  private TileRenderer getTileRenderer(@NotNull MapsforgeTileServer tileServer) {
    File[] maps = tileServer.maps;
    //noinspection LoopStatementThatDoesntLoop
    for (int i = 0, n = maps.length; i < n; i++) {
      File mapFile = maps[i];
      TileRenderer tileRenderer = tileRenderers[i];
      if (tileRenderer == null) {
        tileRenderer = new TileRenderer(tileServer.displayModel, mapFile, tileServer.defaultRenderTheme);
        tileRenderers[i] = tileRenderer;
      }

      // todo support multimap
      return tileRenderer;
    }

    throw new IllegalStateException();
  }
}

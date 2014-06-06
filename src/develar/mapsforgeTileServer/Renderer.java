package develar.mapsforgeTileServer;

import org.jetbrains.annotations.NotNull;
import org.mapsforge.core.model.Tile;

import java.awt.image.BufferedImage;
import java.io.File;

public final class Renderer {
  private TileRendererImpl[] tileRenderers;

  @NotNull
  public BufferedImage render(@NotNull Tile tile, @NotNull MapsforgeTileServer tileServer) {
    if (tileRenderers == null) {
      tileRenderers = new TileRendererImpl[tileServer.maps.length];
    }
    return getTileRenderer(tileServer).render(tile);
  }

  @NotNull
  private TileRenderer getTileRenderer(@NotNull MapsforgeTileServer tileServer) {
    File[] maps = tileServer.maps;
    //noinspection LoopStatementThatDoesntLoop
    for (int i = 0, n = maps.length; i < n; i++) {
      File mapFile = maps[i];
      TileRendererImpl tileRenderer = tileRenderers[i];
      if (tileRenderer == null) {
        tileRenderer = new TileRendererImpl(tileServer.displayModel, mapFile);
        tileRenderer.setXmlRenderTheme(tileServer.defaultRenderTheme);
        tileRenderers[i] = tileRenderer;
      }

      // todo support multimap
      return tileRenderer;
    }

    throw new IllegalStateException();
  }
}

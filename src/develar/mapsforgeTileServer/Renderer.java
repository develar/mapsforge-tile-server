package develar.mapsforgeTileServer;

import org.jetbrains.annotations.NotNull;
import org.mapsforge.core.model.Tile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

public final class Renderer {
  private TileRendererImpl[] tileRenderers;

  public byte[] render(@NotNull Tile tile, @NotNull MapsforgeTileServer tileServer) throws IOException {
    if (tileRenderers == null) {
      tileRenderers = new TileRendererImpl[tileServer.maps.length];
    }

    TileRenderer tileRenderer = getTileRenderer(tileServer);
    BufferedImage bufferedImage = tileRenderer.render(tile);
    ByteArrayOutputStream out = new ByteArrayOutputStream(8 * 1024);
    ImageIO.write(bufferedImage, "png", out);
    byte[] bytes = out.toByteArray();
    out.close();
    return bytes;
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

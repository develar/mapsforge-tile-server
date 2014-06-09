package org.develar.mapsforgeTileServer;

import org.jetbrains.annotations.NotNull;
import org.mapsforge.core.graphics.TileBitmap;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.awt.AwtGraphicFactory;
import org.mapsforge.map.layer.renderer.DatabaseRenderer;
import org.mapsforge.map.layer.renderer.RendererJob;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.reader.MapDatabase;
import org.mapsforge.map.reader.header.FileOpenResult;
import org.mapsforge.map.rendertheme.XmlRenderTheme;

import java.awt.image.BufferedImage;
import java.io.File;

public class TileRenderer {
  private final DisplayModel displayModel;
  private final File mapFile;
  private XmlRenderTheme renderTheme;

  private final DatabaseRenderer databaseRenderer;

  private final String mapFileLastModified;
  private final String renderThemeFileLastModified;

  public TileRenderer(@NotNull DisplayModel displayModel, @NotNull File mapFile, XmlRenderTheme renderTheme) {
    this.displayModel = displayModel;
    MapDatabase mapDatabase = new MapDatabase();
    databaseRenderer = new DatabaseRenderer(mapDatabase, MapsforgeTileServer.GRAPHIC_FACTORY);

    this.mapFile = mapFile;
    this.renderTheme = renderTheme;

    mapFileLastModified = Long.toUnsignedString(mapFile.lastModified(), 32);
    // todo theme file last modified instead of hashcode
    renderThemeFileLastModified = Integer.toUnsignedString(renderTheme.hashCode(), 32);

    FileOpenResult result = mapDatabase.openFile(mapFile);
    if (!result.isSuccess()) {
      throw new IllegalArgumentException(result.getErrorMessage());
    }
  }

  @NotNull
  public String computeETag(@NotNull TileRequest tile, @NotNull StringBuilder stringBuilder) {
    stringBuilder.setLength(0);
    stringBuilder.append(mapFileLastModified).append('@');
    stringBuilder.append(renderThemeFileLastModified).append('@');
    stringBuilder.append(Integer.toString(tile.flags, 32)).append('.');
    stringBuilder.append(Integer.toString(tile.zoomLevel, 32)).append('-').append(Long.toUnsignedString(tile.tileX, 32)).append('-').append(Long.toUnsignedString(tile.tileX, 32));
    return stringBuilder.toString();
  }

  @NotNull
  public BufferedImage render(@NotNull Tile tile) {
    RendererJob rendererJob = new RendererJob(tile, mapFile, renderTheme, displayModel, 1, false);
    TileBitmap bitmap = databaseRenderer.executeJob(rendererJob);
    bitmap.decrementRefCount();
    return AwtGraphicFactory.getBufferedImage(bitmap);
  }
}

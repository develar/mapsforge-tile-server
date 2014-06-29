package org.develar.mapsforgeTileServer;

import org.develar.mapsforgeTileServer.pixi.PixiGraphicFactory;
import org.jetbrains.annotations.NotNull;
import org.mapsforge.core.graphics.TileBitmap;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.awt.AwtGraphicFactory;
import org.mapsforge.map.layer.renderer.DatabaseRenderer;
import org.mapsforge.map.layer.renderer.TileServerDatabaseRenderer;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.reader.MapDatabase;
import org.mapsforge.map.reader.header.FileOpenResult;
import org.mapsforge.map.rendertheme.rule.RenderTheme;

import java.awt.image.BufferedImage;
import java.io.File;

public class TileRenderer {
  private final DisplayModel displayModel;

  private final DatabaseRenderer databaseRenderer;
  private final RenderTheme vectorRenderTheme;
  // DatabaseRenderer keep reference to canvas and we cannot change it after creation, so, currently, we use different instances
  private TileServerDatabaseRenderer databaseVectorRenderer;

  private final String mapFileLastModified;
  private final String renderThemeEtag;

  public TileRenderer(@NotNull DisplayModel displayModel, @NotNull File mapFile, @NotNull RenderThemeItem renderTheme, @NotNull DatabaseRenderer.TileCacheInfoProvider tileCacheInfoProvider) {
    this.displayModel = displayModel;
    MapDatabase mapDatabase = new MapDatabase();
    databaseRenderer = new DatabaseRenderer(mapDatabase, MapsforgeTileServer.AWT_GRAPHIC_FACTORY, tileCacheInfoProvider);
    vectorRenderTheme = renderTheme.vectorRenderTheme;
    databaseRenderer.setRenderTheme(renderTheme.awtRenderTheme);

    mapFileLastModified = Long.toUnsignedString(mapFile.lastModified(), 32);
    renderThemeEtag = renderTheme.etag;

    FileOpenResult result = mapDatabase.openFile(mapFile);
    if (!result.isSuccess()) {
      throw new IllegalArgumentException(result.getErrorMessage());
    }
  }

  @NotNull
  public BoundingBox getBoundingBox() {
    return databaseRenderer.getMapDatabase().getMapFileInfo().boundingBox;
  }

  @NotNull
  public String computeETag(@NotNull TileRequest tile, @NotNull StringBuilder stringBuilder) {
    stringBuilder.setLength(0);
    stringBuilder.append(mapFileLastModified).append('@');
    stringBuilder.append(renderThemeEtag).append('@');
    stringBuilder.append(Integer.toString(tile.getImageFormat().ordinal(), 32)).append('.');
    stringBuilder.append(Integer.toString(tile.zoomLevel, 32)).append('-').append(Long.toUnsignedString(tile.tileX, 32)).append('-').append(Long.toUnsignedString(tile.tileX, 32));
    return stringBuilder.toString();
  }

  @NotNull
  public TileBitmap renderVector(@NotNull Tile tile) {
    if (databaseVectorRenderer == null) {
      databaseVectorRenderer = new TileServerDatabaseRenderer(databaseRenderer.getMapDatabase(), PixiGraphicFactory.INSTANCE);
      databaseVectorRenderer.setRenderTheme(vectorRenderTheme);
    }
    return databaseVectorRenderer.renderTile(tile, false, false, displayModel);
  }

  @NotNull
  public BufferedImage render(@NotNull Tile tile) {
    return AwtGraphicFactory.getBitmap(databaseRenderer.renderTile(tile, 1, false, false, displayModel));
  }
}

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

public class TileRendererImpl implements TileRenderer {
  private final DisplayModel displayModel;
  private final File mapFile;
  private XmlRenderTheme xmlRenderTheme;

  private final MapDatabase mapDatabase;
  private final DatabaseRenderer databaseRenderer;

  public TileRendererImpl(@NotNull DisplayModel displayModel, @NotNull File mapFile) {
    this.displayModel = displayModel;
    mapDatabase = new MapDatabase();
    databaseRenderer = new DatabaseRenderer(mapDatabase, MapsforgeTileServer.GRAPHIC_FACTORY);

    this.mapFile = mapFile;
    FileOpenResult result = mapDatabase.openFile(mapFile);
    if (!result.isSuccess()) {
      throw new IllegalArgumentException(result.getErrorMessage());
    }
  }

  public MapDatabase getMapDatabase() {
    return mapDatabase;
  }

  public void setXmlRenderTheme(@NotNull XmlRenderTheme xmlRenderTheme) {
    this.xmlRenderTheme = xmlRenderTheme;
  }

  @NotNull
  @Override
  public BufferedImage render(@NotNull Tile tile) {
    RendererJob rendererJob = new RendererJob(tile, mapFile, xmlRenderTheme, displayModel, 1, false);
    TileBitmap bitmap = databaseRenderer.executeJob(rendererJob);
    bitmap.decrementRefCount();
    return AwtGraphicFactory.getBufferedImage(bitmap);
  }
}

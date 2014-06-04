package develar.mapsforgeTileServer;

import org.jetbrains.annotations.NotNull;
import org.mapsforge.core.graphics.TileBitmap;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.awt.AwtGraphicFactoryUtil;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.renderer.DatabaseRenderer;
import org.mapsforge.map.layer.renderer.RendererJob;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.reader.MapDatabase;
import org.mapsforge.map.reader.header.FileOpenResult;
import org.mapsforge.map.rendertheme.XmlRenderTheme;

import java.awt.image.BufferedImage;
import java.io.File;

public class TileRendererImpl implements TileRenderer {
  private final TileCache tileCache;
  @NotNull private final DisplayModel displayModel;
  private XmlRenderTheme xmlRenderTheme;

  private final MapDatabase mapDatabase;
  private final DatabaseRenderer databaseRenderer;
  private File mapFile;

  public TileRendererImpl(@NotNull TileCache tileCache, @NotNull DisplayModel displayModel) {
    this.tileCache = tileCache;
    this.displayModel = displayModel;
    mapDatabase = new MapDatabase();
    databaseRenderer = new DatabaseRenderer(mapDatabase, MapsforgeTileServer.GRAPHIC_FACTORY);
  }

  public MapDatabase getMapDatabase() {
    return mapDatabase;
  }

  public void setXmlRenderTheme(@NotNull XmlRenderTheme xmlRenderTheme) {
    this.xmlRenderTheme = xmlRenderTheme;
  }

  public void setMapFile(@NotNull File mapFile) {
    this.mapFile = mapFile;
    FileOpenResult result = mapDatabase.openFile(mapFile);
    if (!result.isSuccess()) {
      throw new IllegalArgumentException(result.getErrorMessage());
    }
  }

  @NotNull
  @Override
  public synchronized BufferedImage render(@NotNull Tile tile) {
    RendererJob rendererJob = new RendererJob(tile, mapFile, xmlRenderTheme, displayModel, 1, false);
    TileBitmap bitmap = tileCache.get(rendererJob);
    if (bitmap == null) {
      bitmap = databaseRenderer.executeJob(rendererJob);
      assert bitmap != null;
      tileCache.put(rendererJob, bitmap);
      bitmap.decrementRefCount();
    }
    else {
      bitmap.decrementRefCount();
    }

    return AwtGraphicFactoryUtil.getBufferedImage(bitmap);
  }
}

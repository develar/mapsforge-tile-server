package org.develar.mapsforgeTileServer

import org.develar.mapsforgeTileServer.pixi.PixiGraphicFactory
import org.mapsforge.core.model.BoundingBox
import org.mapsforge.core.model.Tile
import org.mapsforge.map.awt.AwtGraphicFactory
import org.mapsforge.map.layer.renderer.DatabaseRenderer
import org.mapsforge.map.layer.renderer.TileServerDatabaseRenderer
import org.mapsforge.map.model.DisplayModel
import org.mapsforge.map.reader.MapDatabase
import org.mapsforge.map.rendertheme.rule.RenderTheme

import java.awt.image.BufferedImage
import java.io.File

public class TileRenderer(private val displayModel:DisplayModel, mapFile:File, renderTheme:RenderThemeItem, tileCacheInfoProvider:DatabaseRenderer.TileCacheInfoProvider) {
  private val databaseRenderer:DatabaseRenderer
  private val vectorRenderTheme:RenderTheme
  // DatabaseRenderer keep reference to canvas and we cannot change it after creation, so, currently, we use different instances
  private var databaseVectorRenderer:TileServerDatabaseRenderer? = null

  private val mapFileLastModified:String
  private val renderThemeEtag:String

  public val boundingBox:BoundingBox
    get() = databaseRenderer.getMapDatabase().getMapFileInfo().boundingBox

  {
    val mapDatabase = MapDatabase()
    databaseRenderer = DatabaseRenderer(mapDatabase, AWT_GRAPHIC_FACTORY, tileCacheInfoProvider)
    vectorRenderTheme = renderTheme.vectorRenderTheme
    databaseRenderer.setRenderTheme(renderTheme.awtRenderTheme)

    mapFileLastModified = java.lang.Long.toUnsignedString(mapFile.lastModified(), 32)
    renderThemeEtag = renderTheme.etag

    val result = mapDatabase.openFile(mapFile)
    if (!result.isSuccess()) {
      throw IllegalArgumentException(result.getErrorMessage())
    }
  }

  public fun computeETag(tile:TileRequest, stringBuilder:StringBuilder):String {
    stringBuilder.setLength(0)
    stringBuilder.append(mapFileLastModified).append('@')
    stringBuilder.append(renderThemeEtag).append('@')
    stringBuilder.append(Integer.toString(tile.getImageFormat().ordinal(), 32)).append('.')
    stringBuilder.append(Integer.toString(tile.zoomLevel.toInt(), 32)).append('-').append(java.lang.Long.toUnsignedString(tile.tileX.toLong(), 32)).append('-').append(java.lang.Long.toUnsignedString(tile.tileX.toLong(), 32))
    return stringBuilder.toString()
  }

  public fun renderVector(tile:Tile, pixiGraphicFactory:PixiGraphicFactory):ByteArray {
    var renderer = databaseVectorRenderer
    if (renderer == null) {
      renderer = TileServerDatabaseRenderer(databaseRenderer.getMapDatabase(), pixiGraphicFactory)
      renderer!!.renderTheme = vectorRenderTheme
      databaseVectorRenderer = renderer;
    }
    return renderer!!.renderTile(tile)
  }

  public fun render(tile:Tile):BufferedImage {
    return AwtGraphicFactory.getBitmap(databaseRenderer.renderTile(tile, 1f, false, false, displayModel))
  }
}

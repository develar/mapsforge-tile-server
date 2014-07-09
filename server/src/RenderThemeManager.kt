package org.develar.mapsforgeTileServer

import java.util.LinkedHashMap
import java.util.HashMap
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.function.Consumer
import com.badlogic.gdx.tools.texturepacker.TexturePacker.Settings
import com.badlogic.gdx.tools.texturepacker.TexturePacker
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import org.develar.mapsforgeTileServer.pixi.ByteArrayOutput
import com.badlogic.gdx.utils.ObjectIntMap
import java.util.Locale
import org.mapsforge.map.rendertheme.ExternalRenderTheme
import org.mapsforge.map.rendertheme.rule.RenderThemeHandler
import org.develar.mapsforgeTileServer.pixi.PixiGraphicFactory
import org.mapsforge.core.graphics.GraphicFactory
import org.mapsforge.map.model.DisplayModel
import org.xmlpull.v1.XmlPullParser
import org.mapsforge.map.rendertheme.renderinstruction.Symbol
import org.develar.mapsforgeTileServer.pixi.PixiSymbol
import com.badlogic.gdx.files.FileHandle
import org.mapsforge.map.rendertheme.rule.RenderThemeFactory
import org.mapsforge.map.rendertheme.rule.RenderThemeBuilder
import org.mapsforge.map.rendertheme.rule.RenderTheme
import org.mapsforge.core.graphics.Bitmap
import org.develar.mapsforgeTileServer.pixi.TextureAtlasInfo

abstract class MyRenderThemeFactory : RenderThemeFactory {
  override fun create(renderThemeBuilder: RenderThemeBuilder): RenderTheme {
    return RenderTheme(renderThemeBuilder, UglyGuavaCacheBuilderJavaWrapper.createCache(), UglyGuavaCacheBuilderJavaWrapper.createCache())
  }
}

private val AWT_RENDER_THEME_FACTORY = object : MyRenderThemeFactory() {
  private class AwtSymbol(graphicFactory: GraphicFactory?, displayModel: DisplayModel, qName: String, pullParser: XmlPullParser, relativePathPrefix: String) : Symbol(graphicFactory, displayModel, qName, pullParser) {
    private val _bitmap = createBitmap(relativePathPrefix, src)

    override fun getBitmap(): Bitmap? = _bitmap
  }

  override fun createSymbol(graphicFactory: GraphicFactory?, displayModel: DisplayModel, qName: String, pullParser: XmlPullParser, relativePathPrefix: String): Symbol {
    return AwtSymbol(graphicFactory, displayModel, qName, pullParser, relativePathPrefix)
  }
}

class RenderThemeManager(renderThemeFiles: Array<Path>, displayModel: DisplayModel) {
  val themes = LinkedHashMap<String, RenderThemeItem>()
  private val generatedResources: File
  private val resourceRoots = HashMap<File, String>()

  val defaultTheme: RenderThemeItem

  {
    val atlasDir = System.getProperty("mts.atlasDir")
    generatedResources = if (atlasDir == null) Files.createTempDirectory("mts-render-themes-resources").toFile() else File(atlasDir)

    val themeResourceRootNameToTextureAtlasInfo = HashMap<String, TextureAtlasInfo>()
    processPaths(renderThemeFiles, ".xml", 2, object : Consumer<Path> {
      override fun accept(path: Path) {
        addRenderTheme(path, themeResourceRootNameToTextureAtlasInfo, displayModel)
      }
    })

    if (themes.isEmpty()) {
      throw IllegalStateException("No render theme specified")
    }

    var themeName = "elevate"
    var defaultTheme = themes.get(themeName)
    if (defaultTheme == null) {
      themeName = themes.keySet().iterator().next()
      defaultTheme = themes.get(themeName)
    }

    LOG.info("Use " + themeName + " as default theme")

    this.defaultTheme = defaultTheme!!
  }

  private fun addRenderTheme(path: Path, themeResourceRootNameToTextureAtlasInfo: HashMap<String, TextureAtlasInfo>, displayModel: DisplayModel) {
    val parent = path.getParent()!!.toFile()
    val parentName = parent.getName()
    val textureAtlasInfo: TextureAtlasInfo
    if (resourceRoots.put(parent, parentName) == null) {
      val settings = Settings()
      settings.limitMemory = false
      TexturePacker.process(settings, File(parent, "ele_res").path, generatedResources.path, parentName)

      val atlasData = TextureAtlas.TextureAtlasData(FileHandle(File(generatedResources, parentName + ".atlas")), FileHandle(generatedResources), false)
      val pages = atlasData.getPages()
      if (pages.size > 1) {
        throw UnsupportedOperationException("Only one page supported")
      }

      val byteOut = ByteArrayOutput()
      val regions = atlasData.getRegions()
      byteOut.writeUnsighedVarInt(regions.size)
      val nameToId = ObjectIntMap<String>(regions.size)
      for (i in 0..regions.size - 1) {
        val image = regions.get(i)
        nameToId.put(image.name, i)
        byteOut.writeUnsighedVarInt(image.left)
        byteOut.writeUnsighedVarInt(image.top)
        byteOut.writeUnsighedVarInt(image.width)
        byteOut.writeUnsighedVarInt(image.height)
      }

      byteOut.writeTo(File(generatedResources, parentName + ".atl"))

      textureAtlasInfo = TextureAtlasInfo(nameToId, regions)
      themeResourceRootNameToTextureAtlasInfo.put(parentName, textureAtlasInfo)
    }
    else {
      textureAtlasInfo = themeResourceRootNameToTextureAtlasInfo.get(parentName)!!
    }

    val fileName = path.getFileName().toString()
    val name = fileName.substring(0, fileName.length() - ".xml".length()).toLowerCase(Locale.ENGLISH)
    val xmlRenderTheme = ExternalRenderTheme(path.toFile())
    val etag = "$name@${java.lang.Long.toUnsignedString(Files.getLastModifiedTime(path).toMillis(), 32)}"

    val vectorRenderTheme = RenderThemeHandler.getRenderTheme(PixiGraphicFactory.INSTANCE, displayModel, xmlRenderTheme, object: MyRenderThemeFactory() {
      override fun createSymbol(graphicFactory: GraphicFactory?, displayModel: DisplayModel, qName: String, pullParser: XmlPullParser, relativePathPrefix: String): Symbol? {
        return PixiSymbol(displayModel, qName, pullParser, relativePathPrefix, textureAtlasInfo)
      }
    })
    vectorRenderTheme.scaleTextSize(1f)
    // scale depends on zoom, but we cannot set it on each "render tile" invocation - render theme must be immutable,
    // it is client reponsibility to do scaling
    vectorRenderTheme.scaleStrokeWidth(1f)

    val renderTheme = RenderThemeHandler.getRenderTheme(AWT_GRAPHIC_FACTORY, displayModel, xmlRenderTheme, AWT_RENDER_THEME_FACTORY)
    renderTheme.scaleTextSize(1f)
    themes.put(name, RenderThemeItem(renderTheme, vectorRenderTheme, etag))
  }

  fun requestToFile(uri: String): File? {
    for ((file, name) in resourceRoots) {
      if (uri.startsWith(name, 1)) {
        if (uri[name.length + 1] == '.') {
          // atlas
          return File(generatedResources, uri.substring(1))
        }
        else {
          // render theme resources (actually, our client doesn't use it)
          return File(file, uri.substring(name.length + 2))
        }
      }
    }
    return null
  }

  fun dispose(): Unit {
    val files = generatedResources.listFiles();
    if (files != null) {
      for (child in files) {
        child.delete();
      }
    }
    generatedResources.delete();
  }
}
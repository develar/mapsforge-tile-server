package org.develar.mapsforgeTileServer

import com.badlogic.gdx.tools.texturepacker.TexturePacker
import com.badlogic.gdx.tools.texturepacker.TexturePacker.Settings
import io.netty.handler.codec.http.FullHttpRequest
import org.develar.mapsforgeTileServer.http.isWebpSupported
import org.develar.mapsforgeTileServer.pixi.*
import org.mapsforge.core.graphics.Bitmap
import org.mapsforge.core.graphics.GraphicFactory
import org.mapsforge.map.model.DisplayModel
import org.mapsforge.map.rendertheme.ExternalRenderTheme
import org.mapsforge.map.rendertheme.renderinstruction.Symbol
import org.mapsforge.map.rendertheme.rule.RenderTheme
import org.mapsforge.map.rendertheme.rule.RenderThemeBuilder
import org.mapsforge.map.rendertheme.rule.RenderThemeFactory
import org.mapsforge.map.rendertheme.rule.RenderThemeHandler
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.function.Consumer

abstract class MyRenderThemeFactory : RenderThemeFactory {
  override fun create(renderThemeBuilder:RenderThemeBuilder):RenderTheme {
    return RenderTheme(renderThemeBuilder, UglyGuavaCacheBuilderJavaWrapper.createCache(), UglyGuavaCacheBuilderJavaWrapper.createCache())
  }
}

private val AWT_RENDER_THEME_FACTORY = object : MyRenderThemeFactory() {
  private class AwtSymbol(graphicFactory:GraphicFactory?, displayModel:DisplayModel, qName:String, pullParser:XmlPullParser, relativePathPrefix:String) : Symbol(graphicFactory, displayModel, qName, pullParser) {
    private val _bitmap = createBitmap(relativePathPrefix, src)

    override fun getBitmap():Bitmap? = _bitmap
  }

  override fun createSymbol(graphicFactory:GraphicFactory?, displayModel:DisplayModel, qName:String, pullParser:XmlPullParser, relativePathPrefix:String):Symbol {
    return AwtSymbol(graphicFactory, displayModel, qName, pullParser, relativePathPrefix)
  }
}

class RenderThemeManager(renderThemeFiles:Array<Path>, displayModel:DisplayModel) {
  val themes = LinkedHashMap<String, RenderThemeItem>()
  private val generatedResources:File

  private val generatedFiles = HashMap<String, File>()

  val pixiGraphicFactory:PixiGraphicFactory

  val defaultTheme:RenderThemeItem

  {
    val atlasDir = System.getProperty("mts.atlasDir")
    generatedResources = if (atlasDir == null) Files.createTempDirectory("mts-render-themes-resources").toFile() else File(atlasDir)

    val texturePackerSettings = Settings()
    texturePackerSettings.limitMemory = false
    // https://www.khronos.org/registry/webgl/sdk/tests/conformance/limits/gl-max-texture-dimensions.html
    // safari 8192
    // chrome (desktop and android) 4096
    texturePackerSettings.maxWidth = 4096
    texturePackerSettings.maxHeight = 4096

    val fontManager = generateFonts(texturePackerSettings)
    pixiGraphicFactory = PixiGraphicFactory(fontManager)

    val themeResourceRootNameToTextureAtlasInfo = HashMap<String, TextureAtlasInfo>()
    processPaths(renderThemeFiles, ".xml", 2, object : Consumer<Path> {
      override fun accept(path:Path) {
        addRenderTheme(path, themeResourceRootNameToTextureAtlasInfo, displayModel, texturePackerSettings)
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

    org.develar.mapsforgeTileServer.LOG.info("Use " + themeName + " as default theme")

    this.defaultTheme = defaultTheme!!
  }

  private fun generateFonts(texturePackerSettings:Settings): FontManager {
    org.develar.mapsforgeTileServer.LOG.info("Generate fonts")
    val fonts = ArrayList<FontInfo>()
    val fontsDir = File(System.getProperty("mts.fontsDir")!!)
    val fontToRegionName = HashMap<FontInfo, String>()
    for (filename in fontsDir.list()!!) {
      if (filename.endsWith(".fnt")) {
        val font = parseFontInfo(File(fontsDir, filename), fonts.size())
        fonts.add(font)
        fontToRegionName[font] = filename.substring(0, filename.length() - ".fnt".length())
      }
    }

    val packFileName = "fonts"
    TexturePacker.process(texturePackerSettings, fontsDir.path, generatedResources.path, packFileName)
    val textureAtlas = convertAtlas("fonts", generatedResources)
    generateFontInfo(fonts, addToGeneratedFiles(packFileName), textureAtlas, fontToRegionName)
    return FontManager(fonts.sortBy(object : Comparator<FontInfo> {
      override fun compare(o1:FontInfo, o2:FontInfo) = o1.size - o2.size
    })
    )
  }

  private fun addToGeneratedFiles(packFileName:String):File {
    val infoFile = File(generatedResources, "$packFileName.info")
    generatedFiles["/$packFileName.info"] = infoFile
    generatedFiles["/$packFileName.atl"] = File(generatedResources, "$packFileName.atl")
    generatedFiles["/$packFileName.png"] = File(generatedResources, "$packFileName.png")
    if (WEBP_PARAM != null) {
      generatedFiles["/$packFileName.webp"] = File(generatedResources, "$packFileName.webp")
    }
    return infoFile
  }

  private fun addRenderTheme(path:Path,
                             themeResourceRootNameToTextureAtlasInfo:HashMap<String, TextureAtlasInfo>,
                             displayModel:DisplayModel,
                             texturePackerSettings:TexturePacker.Settings) {
    val parent = path.getParent()!!.toFile()
    val parentName = parent.getName()
    var textureAtlasInfo = themeResourceRootNameToTextureAtlasInfo.get(parentName)
    if (textureAtlasInfo == null) {
      org.develar.mapsforgeTileServer.LOG.info("Generate render theme resources")
      TexturePacker.process(texturePackerSettings, File(parent, "ele_res").path, generatedResources.path, parentName)
      textureAtlasInfo = convertAtlas(parentName, generatedResources)
      addToGeneratedFiles(parentName)
      themeResourceRootNameToTextureAtlasInfo.put(parentName, textureAtlasInfo!!)
    }

    val fileName = path.getFileName().toString()
    val name = fileName.substring(0, fileName.length() - ".xml".length()).toLowerCase(Locale.ENGLISH)
    val xmlRenderTheme = ExternalRenderTheme(path.toFile())
    val etag = "$name@${java.lang.Long.toUnsignedString(Files.getLastModifiedTime(path).toMillis(), 32)}"

    val vectorRenderTheme = RenderThemeHandler.getRenderTheme(pixiGraphicFactory, displayModel, xmlRenderTheme, object : MyRenderThemeFactory() {
      override fun createSymbol(graphicFactory:GraphicFactory?, displayModel:DisplayModel, qName:String, pullParser:XmlPullParser, relativePathPrefix:String):Symbol? {
        return PixiSymbol(displayModel, qName, pullParser, relativePathPrefix, textureAtlasInfo!!)
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

  fun requestToFile(url:String, request:FullHttpRequest):File? {
    var file = generatedFiles[url]
    if (file == null && url.lastIndexOf('.') == -1) {
      file = generatedFiles["$url.${if (isWebpSupported(request)) "webp" else "png"}"]
    }
    return file
  }

  fun dispose():Unit {
    val files = generatedResources.listFiles();
    if (files != null) {
      for (child in files) {
        child.delete();
      }
    }
    generatedResources.delete();
  }
}
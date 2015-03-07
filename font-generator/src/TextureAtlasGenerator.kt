package org.develar.mapsforgeTileServer.assetGenerator

import com.badlogic.gdx.tools.texturepacker.TexturePacker
import org.develar.mapsforgeTileServer.pixi.FontInfo
import org.develar.mapsforgeTileServer.pixi.convertAtlas
import org.develar.mapsforgeTileServer.pixi.generateFontInfo
import org.develar.mapsforgeTileServer.pixi.parseFontInfo
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.util.ArrayList
import java.util.HashMap

fun main(args: Array<String>) {
  val texturePackerSettings = TexturePacker.Settings()
  texturePackerSettings.limitMemory = false
  // https://www.khronos.org/registry/webgl/sdk/tests/conformance/limits/gl-max-texture-dimensions.html
  // safari 8192
  // chrome (desktop and android) 4096
  texturePackerSettings.maxWidth = 4096
  texturePackerSettings.maxHeight = 4096

  TextureAtlasGenerator(File(args[1])).generateFonts(texturePackerSettings, File(args[0]), Paths.get(args[2]))
}

class TextureAtlasGenerator(private val generatedResources: File) {
  class object {
    private val LOG: Logger = LoggerFactory.getLogger("FontGenerator")
  }

  fun generateFonts(texturePackerSettings: TexturePacker.Settings, fontsDir: File, renderThemesDir: Path): Unit {
    LOG.info("Generate fonts")
    val fonts = ArrayList<FontInfo>()
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
    generateFontInfo(fonts, File(generatedResources, "$packFileName.info"), textureAtlas, fontToRegionName)

    LOG.info("Generate render theme resources")
    TexturePacker.process(texturePackerSettings, File(File(renderThemesDir.toFile(), "Elevate"), "ele_res").path, generatedResources.path, "Elevate")
    convertAtlas("Elevate", generatedResources)
  }
}
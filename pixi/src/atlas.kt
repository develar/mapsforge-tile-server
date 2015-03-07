package org.develar.mapsforgeTileServer.pixi

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureAtlas.TextureAtlasData.Region
import com.carrotsearch.hppc.ObjectIntMap
import com.carrotsearch.hppc.ObjectIntOpenHashMap
import com.luciad.imageio.webp.WebP
import com.luciad.imageio.webp.WebPWriteParam
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.Locale
import java.util.function.Consumer
import java.util.function.Predicate
import javax.imageio.ImageIO

data class TextureAtlasInfo(private val nameToId: ObjectIntMap<String>, private val regions: com.badlogic.gdx.utils.Array<Region>) {
  fun getRegion(index: Int) = regions[index]

  fun getRegion(name: String) = getRegion(getIndex(name))

  fun getIndex(name: String) = nameToId.getOrDefault(name, -1)
}

public val WEBP_PARAM: WebPWriteParam? = {
  try {
    val result = WebPWriteParam(Locale.ENGLISH)
    result.setCompressionType("Lossless")
    result
  }
  catch(e: Throwable) {
    LOG.warn("Cannot use webp", e)
    null
  }
}()

public fun processPaths(paths: Array<Path>, ext: String, maxDepth: Int, action: Consumer<Path>) {
  for (specifiedPath in paths) {
    if (!Files.exists(specifiedPath)) {
      throw IllegalArgumentException("File does not exist: " + specifiedPath)
    }
    else if (!Files.isReadable(specifiedPath)) {
      throw IllegalArgumentException("Cannot read file: " + specifiedPath)
    }
    else if (Files.isDirectory(specifiedPath)) {
      Files.walk(specifiedPath, maxDepth).filter(object : Predicate<Path> {
        override fun test(path: Path): Boolean = !Files.isDirectory(path) && path.getFileName().toString().endsWith(ext)
      }).forEachOrdered(action)
    }
    else {
      action.accept(specifiedPath)
    }
  }
}

public fun convertAtlas(packFileName: String, generatedResources: File): TextureAtlasInfo {
  val webP = WEBP_PARAM
  if (webP != null) {
    try {
      File(generatedResources, packFileName + ".webp").writeBytes(WebP.encode(webP, ImageIO.read(File(generatedResources, packFileName + ".png"))!!))
    }
    catch (e: Exception) {
      LOG.warn("Cannot encode webp atlas file", e)
    }
  }

  val atlasData = TextureAtlas.TextureAtlasData(FileHandle(File(generatedResources, packFileName + ".atlas")), FileHandle(generatedResources), false)
  val pages = atlasData.getPages()
  if (pages.size > 1) {
    throw UnsupportedOperationException("Only one page supported")
  }

  val byteOut = ByteArrayOutput()
  val regions = atlasData.getRegions()
  byteOut.writeUnsignedVarInt(regions.size)
  val nameToId = ObjectIntOpenHashMap<String>(regions.size)
  for (i in 0..regions.size - 1) {
    val image = regions.get(i)
    nameToId.put(image.name, i)
    byteOut.writeUnsignedVarInt(image.left)
    byteOut.writeUnsignedVarInt(image.top)
    byteOut.writeUnsignedVarInt(image.width)
    byteOut.writeUnsignedVarInt(image.height)
  }

  byteOut.writeTo(File(generatedResources, packFileName + ".atl"))
  return TextureAtlasInfo(nameToId, regions)
}
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
import java.nio.file.Paths
import java.util.Locale
import javax.imageio.ImageIO

data class TextureAtlasInfo(private val nameToId:ObjectIntMap<String>, private val regions:com.badlogic.gdx.utils.Array<Region>) {
  fun getRegion(index:Int) = regions[index]

  fun getRegion(name:String) = getRegion(getIndex(name))

  fun getIndex(name:String) = nameToId.getOrDefault(name, -1)
}

public val WEBP_PARAM: WebPWriteParam? = {
  try {
    val result = WebPWriteParam(Locale.ENGLISH)
    result.setCompressionType("Lossless")
    result
  }
  catch(e:Exception) {
    LOG.warn("Cannot use webp", e)
    null
  }
}()


fun convertAtlas(packFileName:String, generatedResources:File):TextureAtlasInfo {
  if (WEBP_PARAM != null) {
    try {
      Files.write(Paths.get(generatedResources.getPath(), packFileName + ".webp"), WebP.encode(WEBP_PARAM, ImageIO.read(File(generatedResources, packFileName + ".png"))!!))
    }
    catch(e:Exception) {
      LOG.warn("Cannot encode webp", e)
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
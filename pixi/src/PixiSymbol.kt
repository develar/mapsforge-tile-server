package org.develar.mapsforgeTileServer.pixi

import org.mapsforge.core.graphics.Bitmap
import org.mapsforge.map.model.DisplayModel
import org.mapsforge.map.rendertheme.renderinstruction.Symbol
import org.xmlpull.v1.XmlPullParser

import java.io.OutputStream

class PixiSymbol(displayModel:DisplayModel,
                 elementName:String,
                 pullParser:XmlPullParser,
                 relativePathPrefix:String,
                 textureAtlasInfo:TextureAtlasInfo) : Symbol(null, displayModel, elementName, pullParser), Bitmap {
  val index:Int

  {
    val subPath = src!!.substring("file:".length() + 1)
    // relativePathPrefix = dist/renderThemes/Elevate => Elevate as renderer theme file parent directory name
    var slahIndex = subPath.indexOf('/')
    if (slahIndex == -1) {
      slahIndex = subPath.indexOf('\\')
    }
    index = textureAtlasInfo.getIndex(subPath.substring(slahIndex + 1, subPath.lastIndexOf('.')))
    assert(index > -1)
    // release memory
    src = null

    if (width == 0f || height == 0f) {
      val region = textureAtlasInfo.getRegion(index)
      if (width == 0f) {
        width = region.width.toFloat()
      }
      if (height == 0f) {
        height = region.height.toFloat()
      }
    }
  }

  override fun getBitmap():Bitmap {
    return this
  }

  override fun compress(outputStream:OutputStream?):Unit = throw IllegalStateException()

  override fun incrementRefCount() {
  }

  override fun decrementRefCount() {
  }

  override fun getHeight():Int = height.toInt()

  override fun getWidth():Int = width.toInt()

  override fun scaleTo(width:Int, height:Int):Unit = throw IllegalStateException()

  override fun setBackgroundColor(color:Int):Unit = throw IllegalStateException()

  override fun hashCode():Int = index.hashCode()

  override fun equals(other:Any?):Boolean = other is PixiSymbol && other.index == index
}

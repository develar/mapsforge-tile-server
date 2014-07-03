package org.develar.mapsforgeTileServer.pixi

import org.mapsforge.core.graphics.Bitmap
import org.mapsforge.map.model.DisplayModel
import org.mapsforge.map.rendertheme.renderinstruction.Symbol
import org.xmlpull.v1.XmlPullParser

import java.io.IOException
import java.io.OutputStream

public class PixiSymbol(displayModel: DisplayModel, elementName: String, pullParser: XmlPullParser, relativePathPrefix: String) : Symbol(null, displayModel, elementName, pullParser), Bitmap {
  public val path: String

  {
    var lastSlahIndex = relativePathPrefix.lastIndexOf('/')
    if (lastSlahIndex == -1) {
      lastSlahIndex = relativePathPrefix.lastIndexOf('\\')
    }
    path = relativePathPrefix.substring(lastSlahIndex + 1) + src!!.substring("file:".length()).replace('\\', '/')
    // release memory
    src = null
  }

  override fun getBitmap(): Bitmap {
    return this
  }

  throws(javaClass<IOException>())
  override fun compress(outputStream: OutputStream?): Unit = throw IllegalStateException()

  override fun incrementRefCount() {
  }

  override fun decrementRefCount() {
  }

  override fun getHeight(): Int = height.toInt()

  override fun getWidth(): Int = width.toInt()

  override fun scaleTo(width: Int, height: Int): Unit = throw IllegalStateException()

  override fun setBackgroundColor(color: Int): Unit = throw IllegalStateException()
}

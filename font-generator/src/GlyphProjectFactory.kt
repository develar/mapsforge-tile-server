package org.develar.mapsforgeTileServer.fontGenerator

import com.dd.plist.*
import org.mapsforge.core.graphics.FontStyle
import java.io.File

class GlyphProjectFactory(val template:File) {
  private val rootDict:NSDictionary

  private var fileName:NSString? = null
  private var fontAndStyle:NSString? = null
  private var fontAndStyle2:NSString? = null

  private var fillColorData:NSData? = null
  private var strokeColorData:NSData? = null

  private var strokeWidthNumber:NSNumber? = null

  {
    rootDict = PropertyListParser.parse(template) as NSDictionary
    val objects = (rootDict.objectForKey("\$objects") as NSArray).getArray()
    for (i in 0..objects.size() - 1) {
      val value = objects[i]
      when (value) {
        is NSString -> {
          when (value.getContent()) {
            "font-stroke" -> fileName = value
            "AvenirNext-Regular" -> fontAndStyle = value
            "Avenir Next Regular" -> fontAndStyle2 = value
          }
        }
        is NSDictionary -> {
          val data = value.objectForKey("NSRGB")
          if (data is NSData) {
            when (data.getBase64EncodedData()) {
              "MCAwIDEA" -> fillColorData = data
              "MCAxIDAA" -> strokeColorData = data
            }
          }
        }
        is NSNumber -> {
          if (value.doubleValue() == 2.4200000762939453) {
            strokeWidthNumber = value
          }
        }
      }
    }

    if (strokeColorData == null) {
      throw IllegalStateException("Cannot find stroke color data")
    }
  }

  fun createAndSave(file:String, fontStyle:FontStyle, fillColor:Int, strokeWidth:Float, strokeColor:Int):File {
    fileName!!.setContent(file)

    val styleName = when (fontStyle) {
      FontStyle.NORMAL -> "Regular"
      FontStyle.BOLD -> "Bold"
      FontStyle.ITALIC -> "Italic"
      FontStyle.BOLD_ITALIC -> "BoldItalic"
    }
    fontAndStyle!!.setContent("AvenirNext-${styleName}")
    fontAndStyle2!!.setContent("Avenir Next ${if (fontStyle == FontStyle.BOLD_ITALIC) "Bold Italic" else styleName}")

    fillColorData!!.setBytes(rgbaToNsrgb(fillColor))

    if (strokeWidth == -1f) {
      assert(strokeColorData == null)
      assert(strokeWidthNumber == null)
    }
    else {
      strokeColorData!!.setBytes(rgbaToNsrgb(strokeColor))
      strokeWidthNumber!!.setValue(strokeWidth)
    }

    val out = File("$file")
    PropertyListParser.saveAsBinary(rootDict, out)
    return out
  }
}
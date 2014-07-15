package org.develar.mapsforgeTileServer

import com.badlogic.gdx.utils.IntMap
import java.awt.Rectangle
import com.badlogic.gdx.utils.IntIntMap
import java.io.File
import java.io.FileReader
import org.xmlpull.v1.XmlPullParser
import org.kxml2.io.KXmlParser

data
class FontInfo(public val size:Int, public val bold:Boolean, public val italic:Boolean) {
  var chars:IntMap<CharInfo>? = null
}

data
class CharInfo(public val xOffset:Int, public val yOffset:Int, public val xAdvance:Int, public val rect:Rectangle) {
  val kerning = IntIntMap()
}

fun KXmlParser.getIntAttribute(name:String):Int = getAttributeValue(null, name)!!.toInt()
fun KXmlParser.get(name:String):String = getAttributeValue(null, name)!!

fun parseFontInfo(file:File):FontInfo {
  val parser = KXmlParser()
  parser.setInput(FileReader(file))
  var eventType = parser.getEventType()
  var font:FontInfo? = null
  var chars:IntMap<CharInfo>? = null
  do {
    when (eventType) {
      XmlPullParser.START_TAG -> {
        when (parser.getName()!!) {
          "info" -> {
            font = FontInfo(parser["size"].toInt(), parser["bold"] == "1", parser["italic"] == "1")
          }

          "common" -> {
            if (parser["pages"] != "1") {
              throw UnsupportedOperationException("Only one page supported")
            }
          }

          "chars" -> {
            chars = IntMap<CharInfo>(parser["count"].toInt())
          }

          "char" -> {
            val rect = Rectangle(parser.getIntAttribute("x"), parser["y"].toInt(), parser["width"].toInt(), parser["height"].toInt())
            chars!!.put(parser["id"].toInt(), CharInfo(parser["xoffset"].toInt(), parser["yoffset"].toInt(), parser["xadvance"].toInt(), rect))
          }

          "kerning" -> {
            chars!!.get(parser["second"].toInt())?.kerning?.put(parser["first"].toInt(), parser["amount"].toInt())
          }
        }
      }
    }
    eventType = parser.next();
  }
  while (eventType != XmlPullParser.END_DOCUMENT)

  font!!.chars = chars
  return font!!
}

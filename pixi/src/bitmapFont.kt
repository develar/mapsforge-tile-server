package org.develar.mapsforgeTileServer.pixi

import com.badlogic.gdx.utils.IntMap
import java.awt.Rectangle
import com.badlogic.gdx.utils.IntIntMap
import java.io.File
import java.io.FileReader
import org.xmlpull.v1.XmlPullParser
import org.kxml2.io.KXmlParser
import org.mapsforge.core.graphics.FontFamily
import org.mapsforge.core.graphics.FontStyle
import java.util.Comparator
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.awt.Point
import java.io.BufferedOutputStream
import java.io.FileOutputStream
import org.develar.mapsforgeTileServer.UglyKotlin
import java.io.OutputStream

val LOG:Logger = LoggerFactory.getLogger(javaClass<FontManager>())

data
class FontInfo(public val size:Int, public val style:FontStyle) {
  var chars:IntMap<CharInfo>? = null

  fun getCharInfo(codePoint:Int) = chars!!.get(codePoint)
}

data
class CharInfo(public val xOffset:Int, public val yOffset:Int, public val xAdvance:Int, public val rectangle:Rectangle) {
  val kerning = IntIntMap()
}

class FontManager(fonts:List<FontInfo>) {
  val fonts = fonts.sortBy(object: Comparator<FontInfo> {
    override fun compare(o1:FontInfo, o2:FontInfo) = o1.size - o2.size
  })

  fun measureText(text:String, font:FontInfo):Point {
    var x = 0
    var height = 0
    var prevCharCode = -1;
    for (i in 0..text.length - 1) {
      val charCode = text.codePointAt(i)
      val charInfo = font.getCharInfo(charCode)
      if (charInfo == null) {
        LOG.warn("missed char: " + text[i])
        continue
      }

      if (prevCharCode != -1) {
        x += charInfo.kerning.get(prevCharCode, 0);
      }
      x += charInfo.xAdvance;
      prevCharCode = charCode;

      height = Math.max(height, charInfo.rectangle.height + charInfo.yOffset)
    }
    return Point(x, height)
  }

  fun getFont(family:FontFamily, style:FontStyle, size:Int):FontInfo? {
    for (font in fonts) {
      if (font.style == style && font.size == size) {
        return font
      }
    }
    return null
  }
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
            val bold = parser["bold"] == "1"
            val italic = parser["italic"] == "1"
            font = FontInfo(parser["size"].toInt(), when {
              bold && italic -> FontStyle.BOLD_ITALIC
              bold -> FontStyle.BOLD
              italic -> FontStyle.ITALIC
              else -> FontStyle.NORMAL
            })
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

fun generateFontFile(fonts:List<FontInfo>, outFile:File) {
  val out = BufferedOutputStream(FileOutputStream(outFile))
  val buffer = ByteArrayOutput()
  buffer.writeUnsighedVarInt(fonts.size)
  for (i in 0..fonts.size - 1) {
    val font = fonts[i]
    buffer.writeUnsighedVarInt(font.size)
    buffer.write(font.style.ordinal())
    buffer.writeTo(out)
    buffer.reset()

    val chars = font.chars!!
    buffer.writeUnsighedVarInt(chars.size)
    buffer.writeTo(out)
    buffer.reset()

    val sortedCharCodes = UglyKotlin.getSortedKeys(chars)
    var prevCharCode = 0
    for (k in 0..sortedCharCodes.size - 1) {
      val charCode = sortedCharCodes[k]
      val charInfo = chars.get(charCode)!!

      buffer.writeUnsighedVarInt(charCode - prevCharCode)
      buffer.writeTo(out)
      buffer.reset()

      out.write(charInfo.xOffset)
      out.write(charInfo.yOffset)
      out.write(charInfo.xAdvance)

      writeKerningsInfo(charInfo, buffer, out)

      prevCharCode = charCode
    }
  }

  out.close()
}

private fun writeKerningsInfo(charInfo:CharInfo, buffer:ByteArrayOutput, out:OutputStream) {
  buffer.writeUnsighedVarInt(charInfo.kerning.size)
  buffer.writeTo(out)
  buffer.reset()

  val sortedKernings = UglyKotlin.getSortedKeys(charInfo.kerning)
  var prevCharCode = 0
  for (i in 0..sortedKernings.size - 1) {
    val charCode = sortedKernings[i]

    buffer.writeUnsighedVarInt(charCode - prevCharCode)
    buffer.writeTo(out)
    buffer.reset()

    out.write(charInfo.kerning.get(charCode, 0))

    prevCharCode = charCode
  }
}
package org.develar.mapsforgeTileServer.pixi

import java.awt.Rectangle
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
import java.io.OutputStream
import com.carrotsearch.hppc.CharIntMap
import com.carrotsearch.hppc.CharIntOpenHashMap
import com.carrotsearch.hppc.IntObjectOpenHashMap
import com.carrotsearch.hppc.IntObjectMap
import java.util.ArrayList
import com.carrotsearch.hppc.IntIntOpenHashMap
import com.carrotsearch.hppc.IntIntMap

val LOG:Logger = LoggerFactory.getLogger(javaClass<FontManager>())

data
class FontInfo(public val index:Int, public val size:Int, public val style:FontStyle, val chars:List<CharInfo>, private val charToIndex:CharIntMap) {
  fun getCharInfoByChar(char:Char):CharInfo? {
    val index = getCharIndex(char)
    return if (index == -1) null else chars.get(index)
  }

  fun getCharIndex(char:Char) = charToIndex.getOrDefault(char, -1)

  fun getCharInfoByIndex(index:Int) = chars.get(index)
}

data
class CharInfo(public val xOffset:Int, public val yOffset:Int, public val xAdvance:Int, public val rectangle:Rectangle) {
  val kerning = IntIntOpenHashMap()
}

class FontManager(fonts:List<FontInfo>) {
  val fonts = fonts.sortBy(object: Comparator<FontInfo> {
    override fun compare(o1:FontInfo, o2:FontInfo) = o1.size - o2.size
  })

  fun measureText(text:String, font:FontInfo):Point {
    var x = 0
    var height = 0
    var prevCharIndex = -1;
    for (char in text) {
      val charIndex = font.getCharIndex(char)
      if (charIndex == -1) {
        LOG.warn("missed char: " + char + " " + char.toInt())
        continue
      }

      val charInfo = font.getCharInfoByIndex(charIndex)

      if (prevCharIndex != -1) {
        x += charInfo.kerning.getOrDefault(prevCharIndex, 0);
      }
      x += charInfo.xAdvance;
      height = Math.max(height, charInfo.rectangle.height + charInfo.yOffset)

      prevCharIndex = charIndex
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

fun parseFontInfo(file:File, fontIndex:Int):FontInfo {
  val parser = KXmlParser()
  parser.setInput(FileReader(file))
  var eventType = parser.getEventType()
  var chars:MutableList<CharInfo>? = null
  var charToIndex:CharIntMap? = null
  var idToCharInfo:IntObjectMap<CharInfo>? = null
  var idToCharIndex:IntIntMap? = null
  var fontSize:Int? = null
  var fontStyle:FontStyle? = null
  do {
    when (eventType) {
      XmlPullParser.START_TAG -> {
        when (parser.getName()!!) {
          "info" -> {
            fontSize = parser["size"].toInt()

            val bold = parser["bold"] == "1"
            val italic = parser["italic"] == "1"
            fontStyle = when {
              bold && italic -> FontStyle.BOLD_ITALIC
              bold -> FontStyle.BOLD
              italic -> FontStyle.ITALIC
              else -> FontStyle.NORMAL
            }
          }

          "common" -> {
            if (parser["pages"] != "1") {
              throw UnsupportedOperationException("Only one page supported")
            }
          }

          "chars" -> {
            val charCount = parser["count"].toInt()
            charToIndex = CharIntOpenHashMap(charCount)
            chars = ArrayList(charCount)
            idToCharInfo = IntObjectOpenHashMap(charCount)
            idToCharIndex = IntIntOpenHashMap(charCount)
          }

          "char" -> {
            val rect = Rectangle(parser.getIntAttribute("x"), parser["y"].toInt(), parser["width"].toInt(), parser["height"].toInt())
            val charInfo = CharInfo(parser["xoffset"].toInt(), parser["yoffset"].toInt(), parser["xadvance"].toInt(), rect)

            val char:Char
            val letter = parser["letter"]
            char = when (letter) {
              "space" -> ' '
              "&quot;" -> '"'
              "&lt;" -> '<'
              "&gt;" -> '>'
              "&amp;" -> '&'
              else -> {
                assert(letter.length == 1)
                letter[0]
              }
            }

            charToIndex!!.put(char, chars!!.size)
            chars!!.add(charInfo)
            idToCharInfo!!.put(parser["id"].toInt(), charInfo)
          }

          "kerning" -> {
            val charInfo = idToCharInfo!![parser["second"].toInt()]
            if (charInfo != null) {
              charInfo.kerning.put(idToCharIndex!![parser["first"].toInt()], parser["amount"].toInt())
            }
          }
        }
      }
    }
    eventType = parser.next();
  }
  while (eventType != XmlPullParser.END_DOCUMENT)

  return FontInfo(fontIndex, fontSize!!, fontStyle!!, chars!!, charToIndex!!)
}

fun generateFontFile(fonts:List<FontInfo>, outFile:File, textureAtlas:TextureAtlasInfo, fontToRegionName:Map<FontInfo, String>) {
  val out = BufferedOutputStream(FileOutputStream(outFile))
  val buffer = ByteArrayOutput()
  buffer.writeUnsignedVarInt(fonts.size)
  for (font in fonts) {
    //buffer.writeUnsighedVarInt(font.size)
    //buffer.write(font.style.ordinal())
    //buffer.writeTo(out)
    //buffer.reset()

    val chars = font.chars
    buffer.writeUnsignedVarInt(chars.size)
    buffer.writeTo(out)
    buffer.reset()

    var prevX = 0
    var prevY = 0

    val region = textureAtlas.getRegion(fontToRegionName[font]!!)
    for (charInfo in chars) {
      out.write(charInfo.xOffset)
      out.write(charInfo.yOffset)
      out.write(charInfo.xAdvance)

      val x = region.left + charInfo.rectangle.x
      val y = region.top + charInfo.rectangle.y
      buffer.writeSignedVarInt(x - prevX)
      buffer.writeSignedVarInt(y - prevY)
      buffer.writeTo(out)
      buffer.reset()
      prevX = x
      prevY = y

      out.write(charInfo.rectangle.width)
      out.write(charInfo.rectangle.height)

      writeKerningsInfo(charInfo, buffer, out)
    }
  }

  out.close()
}

private fun writeKerningsInfo(charInfo:CharInfo, buffer:ByteArrayOutput, out:OutputStream) {
  buffer.writeUnsignedVarInt(charInfo.kerning.size())
  buffer.writeTo(out)
  buffer.reset()

  if (charInfo.kerning.isEmpty()) {
    return
  }

  val sortedKernings = charInfo.kerning.keys().toArray()
  sortedKernings.sort()
  var prevCharIndex = 0
  for (i in 0..sortedKernings.size - 1) {
    val charIndex = sortedKernings[i]
    buffer.writeUnsignedVarInt(charIndex - prevCharIndex)
    buffer.writeTo(out)
    buffer.reset()

    out.write(charInfo.kerning.get(charIndex))

    prevCharIndex = charIndex
  }
}
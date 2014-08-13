package org.develar.mapsforgeTileServer.pixi

import org.kxml2.io.KXmlParser
import java.io.FileReader
import java.io.File
import org.xmlpull.v1.XmlPullParser
import org.mapsforge.map.rendertheme.XmlUtils
import java.util.Collections
import com.carrotsearch.hppc.IntSet
import com.carrotsearch.hppc.IntOpenHashSet
import org.mapsforge.core.graphics.FontStyle
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.LinkedHashMap

data class Info(val fontStyle:FontStyle, val fontColor:Int, val strokeWidth:Float = -1f, val strokeColor:Int = 0)

// http://stackoverflow.com/questions/10481344/how-to-encode-hexadecimal-color-in-nsrgb-tag-of-xml-file-of-xib
fun rgbaToNsrgb(rgba:Int):ByteArray {
  fun convert(component:Int):String {
    val string = (component.toDouble() / 255).toString()
    val dotIndex = string.indexOf('.')
    return if (dotIndex == -1) string else string.substring(0, Math.min(string.size, 10 + dotIndex + 1))
  }

  val red = convert((rgba shr 16) and 255)
  val green = convert((rgba shr 8) and 255)
  val blue = convert(((rgba shr 0) and 255))
  val bytes = "$red $green $blue".getBytes(StandardCharsets.US_ASCII)
  val paddedBytes = bytes.copyOf(bytes.size + 1)
  paddedBytes[bytes.size] = 0
  return paddedBytes
}

fun rgbaToNsrgbString(rgba:Int) = Base64.getEncoder().encodeToString(rgbaToNsrgb(rgba))

fun main(args:Array<String>) {
//  System.out.println(rgbaToNsrgbString(Color(0, 0, 255, 255).getRGB()))
//  System.out.println(rgbaToNsrgbString(Color(0, 255, 0, 255).getRGB()))
//  System.out.println(rgbaToNsrgbString(Color(136, 136, 136, 255).getRGB()))

  val glyphProjectFactory = GlyphProjectFactory(File("glyph-project-templates/font.GlyphProject"))
  val glyphStrokeProjectFactory = GlyphProjectFactory(File("glyph-project-templates/font-stroke.GlyphProject"))

  val parser = KXmlParser()
  parser.setInput(FileReader(File(args[0])))

  val map = parseTheme(parser, PixiGraphicFactory(FontManager(Collections.emptyList())))

  val out = System.out
  for ((info, fontSizes) in map) {
    val intArray = fontSizes.toArray()
    intArray.sort()

    out.print("${info.fontStyle} ${rgbaToString(info.fontColor)}")
    var name = "${info.fontStyle.name().toLowerCase()}-${toHex(info.fontColor, false)}"
    val bitmapFontProjectFactory:GlyphProjectFactory
    if (info.strokeWidth != -1f) {
      out.print(" ${info.strokeWidth} ${rgbaToString(info.strokeColor)}")

      name += "-${info.strokeWidth}-${toHex(info.strokeColor, false)}"

      bitmapFontProjectFactory = glyphStrokeProjectFactory
    }
    else {
      bitmapFontProjectFactory = glyphProjectFactory
    }

    out.println()

    val glyphProject = bitmapFontProjectFactory.createAndSave("out/glyphProjects/$name.GlyphProject", info.fontStyle, info.fontColor, info.strokeWidth, info.strokeColor)
    for (fontSize in intArray) {
      out.println("\t$fontSize")

      val process = ProcessBuilder("/usr/local/bin/GDCL", glyphProject.getAbsolutePath(), "${args[1]}/$fontSize-$name", "-fs", fontSize.toString(), "-fo", "XML-fnt").inheritIO().start()
      process.waitFor()
    }
  }
}

fun fontStyleNameToEnum(name:String) = when (name) {
  "bold" -> FontStyle.BOLD
  "italic" -> FontStyle.ITALIC
  "bold_italic" -> FontStyle.BOLD_ITALIC
  else -> FontStyle.NORMAL
}

private fun parseTheme(parser:KXmlParser, graphicFactory:PixiGraphicFactory):Map<Info, IntSet> {
  var eventType = parser.getEventType()
  val map = LinkedHashMap<Info, IntSet>()
  do {
    when (eventType) {
      XmlPullParser.START_TAG -> {
        try {
          when (parser.getName()!!) {
            "caption", "pathText" -> {
              val fontSize = parser["font-size"].toInt()
              val fontStyle = fontStyleNameToEnum(parser["font-style"])
              val fontColor = XmlUtils.getColor(graphicFactory, parser["fill"])

              val strokeWidth = parser.getAttributeValue(null, "stroke-width")?.toFloat() ?: -1f
              val key:Info
              if (strokeWidth != -1f) {
                val strokeColor = XmlUtils.getColor(graphicFactory, parser["stroke"])
                key = Info(fontStyle, fontColor, strokeWidth, strokeColor)
              }
              else {
                key = Info(fontStyle, fontColor)
              }

              var fontSizes = map[key]
              if (fontSizes == null) {
                fontSizes = IntOpenHashSet()
                fontSizes!!.add(fontSize)
                map[key] = fontSizes!!
              }
              else {
                fontSizes!!.add(fontSize)
              }
            }
          }
        }
        catch(e:Exception) {
          throw Exception("Line number: ${parser.getLineNumber()}", e)
        }
      }
    }
    eventType = parser.next()
  }
  while (eventType != XmlPullParser.END_DOCUMENT)
  return map
}

private fun rgbaToString(rgba:Int) = "[${(rgba shr 16) and 255} ${(rgba shr 8) and 255} ${(rgba shr 0) and 255} ${toHex(rgba)}]"

private fun toHex(rgba:Int, addPrefix:Boolean = true):String {
  //val alpha = pad(Integer.toHexString((rgba shr 24) and 255));
  val red = pad(Integer.toHexString((rgba shr 16) and 255))
  val green = pad(Integer.toHexString((rgba shr 8) and 255))
  val blue = pad(Integer.toHexString((rgba shr 0) and 255))
  return "${if (addPrefix) '#' else ""}$red$green$blue"
}

private fun pad(s:String) = if (s.length() == 1) "0" + s else s
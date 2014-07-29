package org.develar.mapsforgeTileServer.pixi

import org.mapsforge.core.graphics.*
import org.mapsforge.core.mapelements.SymbolContainer
import org.mapsforge.core.model.Point
import org.mapsforge.map.awt.AwtMatrix

import java.io.IOException
import java.io.InputStream
import org.mapsforge.core.mapelements.MapElementContainer

fun colorToInt(color:Color):Int = when (color) {
  Color.BLACK -> 0
  Color.BLUE -> colorToInt(255, 0, 0, 255)
  Color.GREEN -> colorToInt(255, 0, 255, 0)
  Color.RED -> colorToInt(255, 255, 0, 0)
  Color.TRANSPARENT -> colorToInt(0, 0, 0, 0)
  Color.WHITE -> colorToInt(255, 255, 255, 255)
  else -> throw IllegalArgumentException("unknown color: " + color)
}

fun colorToInt(alpha:Int, red:Int, green:Int, blue:Int):Int = ((alpha and 255) shl 24) or ((red and 255) shl 16) or ((green and 255) shl 8) or (blue and 255)

class PixiGraphicFactory(private val fontManager:FontManager) : GraphicFactory {
  override fun createBitmap(width:Int, height:Int) = throw UnsupportedOperationException()

  override fun createBitmap(width:Int, height:Int, isTransparent:Boolean):Bitmap = throw UnsupportedOperationException()

  override fun createCanvas() = PixiCanvas()

  override fun createColor(color:Color) = colorToInt(color)

  override fun createColor(alpha:Int, red:Int, green:Int, blue:Int) = colorToInt(alpha, red, green, blue)

  override fun createMatrix() = AwtMatrix()

  override fun createPaint() = PixiPaint(fontManager)

  override fun createPath() = PixiPath()

  override fun createPointTextContainer(xy:Point, priority:Int, text:String, paintFront:Paint, paintBack:Paint?, symbolContainer:SymbolContainer?, position:Position, maxTextWidth:Int):MapElementContainer {
    return PixiPointTextContainer(text, xy, priority, paintFront, paintBack, symbolContainer, position, maxTextWidth)
  }

  override fun createResourceBitmap(inputStream:InputStream, hash:Int) = throw IllegalStateException()

  override fun createTileBitmap(inputStream:InputStream, tileSize:Int, isTransparent:Boolean) = throw UnsupportedOperationException()

  override fun createTileBitmap(tileSize:Int, isTransparent:Boolean) = throw UnsupportedOperationException()

  override fun platformSpecificSources(relativePathPrefix:String, src:String) = throw IOException()

  override fun renderSvg(inputStream:InputStream, scaleFactor:Float, width:Int, height:Int, percent:Int, hash:Int) = throw UnsupportedOperationException()
}
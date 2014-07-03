package org.develar.mapsforgeTileServer.pixi

import org.mapsforge.core.graphics.*
import org.mapsforge.core.mapelements.PointTextContainer
import org.mapsforge.core.mapelements.SymbolContainer
import org.mapsforge.core.model.Point
import org.mapsforge.map.awt.AwtMatrix

import java.io.IOException
import java.io.InputStream

public class PixiGraphicFactory() : GraphicFactory {
  class object {
    public val INSTANCE: GraphicFactory = PixiGraphicFactory()
  }

  override fun createBitmap(width: Int, height: Int): Bitmap = PixiBitmap(width, height)

  override fun createBitmap(width: Int, height: Int, isTransparent: Boolean): Bitmap = PixiBitmap(width, height)

  override fun createCanvas(): Canvas = PixiCanvas()

  override fun createColor(color: Color): Int {
    when (color) {
      Color.BLACK -> {
        return 0
      }
      Color.BLUE -> {
        return createColor(255, 0, 0, 255)
      }
      Color.GREEN -> {
        return createColor(255, 0, 255, 0)
      }
      Color.RED -> {
        return createColor(255, 255, 0, 0)
      }
      Color.TRANSPARENT -> {
        return createColor(0, 0, 0, 0)
      }
      Color.WHITE -> {
        return createColor(255, 255, 255, 255)
      }

      else -> {
        throw IllegalArgumentException("unknown color: " + color)
      }
    }
  }

  override fun createColor(alpha: Int, red: Int, green: Int, blue: Int): Int = ((alpha and 255) shl 24) or ((red and 255) shl 16) or ((green and 255) shl 8) or (blue and 255)

  override fun createMatrix(): Matrix = AwtMatrix()

  override fun createPaint(): Paint = PixiPaint()

  override fun createPath(): Path = PixiPath()

  override fun createPointTextContainer(xy: Point, priority: Int, text: String, paintFront: Paint, paintBack: Paint?, symbolContainer: SymbolContainer?, position: Position, maxTextWidth: Int): PointTextContainer {
    return PixiPointTextContainer(xy, priority, text, paintFront, paintBack, symbolContainer, position, maxTextWidth)
  }

  throws(javaClass<IOException>())
  override fun createResourceBitmap(inputStream: InputStream, hash: Int): ResourceBitmap = throw IllegalStateException()

  throws(javaClass<IOException>())
  override fun createTileBitmap(inputStream: InputStream, tileSize: Int, isTransparent: Boolean): TileBitmap = throw UnsupportedOperationException()

  override fun createTileBitmap(tileSize: Int, isTransparent: Boolean): TileBitmap = PixiBitmap(tileSize, tileSize)

  throws(javaClass<IOException>())
  override fun platformSpecificSources(relativePathPrefix: String, src: String): InputStream = throw IOException()

  throws(javaClass<IOException>())
  override fun renderSvg(inputStream: InputStream, scaleFactor: Float, width: Int, height: Int, percent: Int, hash: Int): ResourceBitmap = throw UnsupportedOperationException()
}

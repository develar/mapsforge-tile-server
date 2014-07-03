package org.develar.mapsforgeTileServer.pixi

import org.mapsforge.core.graphics.*

import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage
import java.awt.Font
import java.awt.RenderingHints
import java.awt.Graphics2D

public class PixiPaint() : Paint {
  private var font: Font? = null
  private var fontSize: Int = 0
  private var fontName: String? = null
  private var fontStyle: Int = 0

  var lineWidth: Float = 1f
  var _color: Int = 0
  var _style: Style? = null

  public fun getTextVisualBounds(text: String): Rectangle2D {
    val fontMetrics = TEXT_MEASURER.getGraphics().getFontMetrics(font!!)
    return font!!.createGlyphVector(fontMetrics.getFontRenderContext(), text).getVisualBounds()
  }

  override fun getTextHeight(text: String): Int {
    return getTextVisualBounds(text).getHeight().toInt()
  }

  override fun getTextWidth(text: String): Int {
    return getTextVisualBounds(text).getWidth().toInt()
  }

  override fun isTransparent(): Boolean {
    return getAlpha() == 0
  }

  public fun getAlpha(): Int {
    return ((_color shr 24) and 255)
  }

  override fun setBitmapShader(bitmap: Bitmap) {
    // todo
  }

  override fun setColor(color: Color) {
    _color = PixiGraphicFactory.INSTANCE.createColor(color)
  }

  override fun setColor(color: Int) {
    _color = color
  }

  override fun setDashPathEffect(strokeDasharray: FloatArray) {
    // todo pixijs doesn't support it
  }

  override fun setStrokeCap(cap: Cap) {
    // todo pixijs doesn't support it
  }

  override fun setStrokeJoin(join: Join) {
    // todo pixijs doesn't support it
  }

  override fun setStrokeWidth(strokeWidth: Float) {
    lineWidth = strokeWidth
  }

  override fun setStyle(style: Style) {
    _style = style
  }

  override fun setTextAlign(align: Align) {
  }

  override fun setTextSize(textSize: Float) {
    fontSize = textSize.toInt()
    createFont()
  }

  override fun setTypeface(fontFamily: FontFamily, fontStyle: FontStyle) {
    fontName = getFontName(fontFamily)
    this.fontStyle = getFontStyle(fontStyle)
    createFont()
  }

  private fun createFont() {
    font = if (fontSize > 0) Font(fontName, fontStyle, fontSize) else null
  }

  class object {
    private val TEXT_MEASURER = BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);

    {
      val graphics = TEXT_MEASURER.getGraphics() as Graphics2D
      graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
      graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
      graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON)
    }

    private fun getFontName(fontFamily: FontFamily): String? {
      when (fontFamily) {
        FontFamily.MONOSPACE -> {
          return Font.MONOSPACED
        }
        FontFamily.DEFAULT -> {
          return null
        }
        FontFamily.SANS_SERIF -> {
          return Font.SANS_SERIF
        }
        FontFamily.SERIF -> {
          return Font.SERIF
        }
        else -> {
          throw IllegalArgumentException("unknown fontFamily: " + fontFamily)
        }
      }
    }

    private fun getFontStyle(fontStyle: FontStyle): Int {
      when (fontStyle) {
        FontStyle.BOLD -> {
          return Font.BOLD
        }
        FontStyle.BOLD_ITALIC -> {
          return Font.BOLD or Font.ITALIC
        }
        FontStyle.ITALIC -> {
          return Font.ITALIC
        }
        FontStyle.NORMAL -> {
          return Font.PLAIN
        }
        else -> {
          throw IllegalArgumentException("unknown fontStyle: " + fontStyle)
        }
      }
    }
  }
}

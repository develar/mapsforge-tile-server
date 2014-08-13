package org.develar.mapsforgeTileServer.pixi

import org.mapsforge.core.graphics.*

import java.awt.Font

private fun getFontName(fontFamily:FontFamily):String? {
  when (fontFamily) {
    FontFamily.MONOSPACE -> return Font.MONOSPACED
    FontFamily.DEFAULT -> return null
    FontFamily.SANS_SERIF -> return Font.SANS_SERIF
    FontFamily.SERIF -> return Font.SERIF
    else -> throw IllegalArgumentException("unknown fontFamily: " + fontFamily)
  }
}

private fun getFontStyle(fontStyle:FontStyle):Int {
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
    else -> throw IllegalArgumentException("unknown fontStyle: " + fontStyle)
  }
}

class PixiPaint(val fontManager:FontManager) : Paint {
  var font:FontInfo? = null
    private set

  var fontSize:Int = 0
    private set
  var fontFamily = FontFamily.DEFAULT
    private set
  var fontStyle = FontStyle.NORMAL
    private set

  var lineWidth:Float = 1f
  var _color:Int = 0
    private set

  var _style:Style? = null

  fun getTextVisualBounds(text:String) = fontManager.measureText(text, font!!)

  override fun getTextHeight(text:String) = getTextVisualBounds(text).x

  override fun getTextWidth(text:String) = getTextVisualBounds(text).y

  override fun isTransparent() = getAlpha() == 0

  fun getAlpha() = ((_color shr 24) and 255)

  override fun setBitmapShader(bitmap:Bitmap) {
    // todo
  }

  override fun setColor(color:Color) {
    _color = colorToRgba(color)
    if (_color == -16777216) {
      var i = 3;
      i++;
    }
  }

  override fun setColor(color:Int) {
    _color = color
    if (_color == -16777216) {
      var i = 3;
      i++;
    }
  }

  override fun setDashPathEffect(strokeDasharray:FloatArray) {
    // todo pixijs doesn't support it
  }

  override fun setStrokeCap(cap:Cap) {
    // todo pixijs doesn't support it
  }

  override fun setStrokeJoin(join:Join) {
    // todo pixijs doesn't support it
  }

  override fun setStrokeWidth(strokeWidth:Float) {
    lineWidth = strokeWidth
  }

  override fun setStyle(style:Style) {
    _style = style
  }

  override fun setTextAlign(align:Align) {
  }

  override fun setTextSize(textSize:Float) {
    fontSize = textSize.toInt()
    createFont()
  }

  override fun setTypeface(fontFamily:FontFamily, fontStyle:FontStyle) {
    this.fontFamily = fontFamily
    this.fontStyle = fontStyle
    createFont()
  }

  private fun createFont() {
    if (fontSize > 0) {
      font = fontManager.getFont(fontFamily, fontStyle, fontSize)
      if (font == null) {
        LOG.error("Unknown font " + fontFamily + " " + fontStyle + " " + fontSize)
      }
    }
    else {
      font = null
    }
  }
}

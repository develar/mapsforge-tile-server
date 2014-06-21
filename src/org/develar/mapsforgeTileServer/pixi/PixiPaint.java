package org.develar.mapsforgeTileServer.pixi;

import org.mapsforge.core.graphics.*;
import org.mapsforge.core.graphics.Color;
import org.mapsforge.core.graphics.Paint;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

public class PixiPaint implements Paint {
  private static final BufferedImage TEXT_MEASURER = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);

  private Font font;
  private int fontSize;
  private String fontName;
  private int fontStyle;

  float lineWidth = 1;
  int color;
  Style style;

  static {
    Graphics2D graphics = (Graphics2D)TEXT_MEASURER.getGraphics();
    graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
  }

  private static String getFontName(FontFamily fontFamily) {
    switch (fontFamily) {
      case MONOSPACE:
        return Font.MONOSPACED;
      case DEFAULT:
        return null;
      case SANS_SERIF:
        return Font.SANS_SERIF;
      case SERIF:
        return Font.SERIF;
      default:
        throw new IllegalArgumentException("unknown fontFamily: " + fontFamily);
    }
  }

  private static int getFontStyle(FontStyle fontStyle) {
    switch (fontStyle) {
      case BOLD:
        return Font.BOLD;
      case BOLD_ITALIC:
        return Font.BOLD | Font.ITALIC;
      case ITALIC:
        return Font.ITALIC;
      case NORMAL:
        return Font.PLAIN;
      default:
        throw new IllegalArgumentException("unknown fontStyle: " + fontStyle);
    }
  }

  private Rectangle2D getTextVisualBounds(String text) {
    FontMetrics fontMetrics = TEXT_MEASURER.getGraphics().getFontMetrics(font);
    return font.createGlyphVector(fontMetrics.getFontRenderContext(), text).getVisualBounds();
  }

  @Override
  public int getTextHeight(String text) {
    return (int)getTextVisualBounds(text).getHeight();
  }

  @Override
  public int getTextWidth(String text) {
    return (int)getTextVisualBounds(text).getWidth();
  }

  @Override
  public boolean isTransparent() {
    return getAlpha() == 0;
  }

  public int getAlpha() {
    return ((color >> 24) & 0xff);
  }

  @Override
  public void setBitmapShader(Bitmap bitmap) {
    // todo
  }

  @Override
  public void setColor(Color color) {
    this.color = PixiGraphicFactory.INSTANCE.createColor(color);
  }

  @Override
  public void setColor(int color) {
    this.color = color;
  }

  @Override
  public void setDashPathEffect(float[] strokeDasharray) {
    // todo pixijs doesn't support it
  }

  @Override
  public void setStrokeCap(Cap cap) {
    // todo pixijs doesn't support it
  }

  @Override
  public void setStrokeJoin(Join join) {
    // todo pixijs doesn't support it
  }

  @Override
  public void setStrokeWidth(float strokeWidth) {
    lineWidth = strokeWidth;
  }

  @Override
  public void setStyle(Style style) {
    this.style = style;
  }

  @Override
  public void setTextAlign(Align align) {
  }

  @Override
  public void setTextSize(float textSize) {
    fontSize = (int)textSize;
    createFont();
  }

  @Override
  public void setTypeface(FontFamily fontFamily, FontStyle fontStyle) {
    fontName = getFontName(fontFamily);
    this.fontStyle = getFontStyle(fontStyle);
    createFont();
  }

  private void createFont() {
    font = fontSize > 0 ? new Font(fontName, fontStyle, fontSize) : null;
  }
}

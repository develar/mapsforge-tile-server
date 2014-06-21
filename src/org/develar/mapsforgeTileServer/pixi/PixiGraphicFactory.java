package org.develar.mapsforgeTileServer.pixi;

import org.mapsforge.core.graphics.*;
import org.mapsforge.core.mapelements.PointTextContainer;
import org.mapsforge.core.mapelements.SymbolContainer;
import org.mapsforge.core.model.Point;
import org.mapsforge.map.awt.AwtMatrix;

import java.io.IOException;
import java.io.InputStream;

public class PixiGraphicFactory implements GraphicFactory {
  public static final GraphicFactory INSTANCE = new PixiGraphicFactory();

  @Override
  public Bitmap createBitmap(int width, int height) {
    return new PixiBitmap(width, height);
  }

  @Override
  public Bitmap createBitmap(int width, int height, boolean isTransparent) {
    return new PixiBitmap(width, height);
  }

  @Override
  public Canvas createCanvas() {
    return new PixiCanvas();
  }

  @Override
  public int createColor(Color color) {
    switch (color) {
      case BLACK:
        return 0;
      case BLUE:
        return createColor(255, 0, 0, 255);
      case GREEN:
        return createColor(255, 0, 255, 0);
      case RED:
        return createColor(255, 255, 0, 0);
      case TRANSPARENT:
        return createColor(0, 0, 0, 0);
      case WHITE:
        return createColor(255, 255, 255, 255);

      default:
        throw new IllegalArgumentException("unknown color: " + color);
    }
  }

  @Override
  public int createColor(int alpha, int red, int green, int blue) {
    return ((alpha & 0xFF) << 24) |
      ((red & 0xFF) << 16) |
      ((green & 0xFF) << 8) |
      (blue & 0xFF);
  }

  @Override
  public Matrix createMatrix() {
    return new AwtMatrix();
  }

  @Override
  public Paint createPaint() {
    return new PixiPaint();
  }

  @Override
  public Path createPath() {
    return new PixiPath();
  }

  @Override
  public PointTextContainer createPointTextContainer(Point xy, int priority, String text, Paint paintFront, Paint paintBack, SymbolContainer symbolContainer, Position position, int maxTextWidth) {
    return new PixiPointTextContainer(xy, priority, text, paintFront, paintBack, symbolContainer, position, maxTextWidth);
  }

  @Override
  public ResourceBitmap createResourceBitmap(InputStream inputStream, int hash) throws IOException {
    return new PixiResourceBitmap();
  }

  @Override
  public TileBitmap createTileBitmap(InputStream inputStream, int tileSize, boolean isTransparent) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public TileBitmap createTileBitmap(int tileSize, boolean isTransparent) {
    return new PixiBitmap(tileSize, tileSize);
  }

  @Override
  public InputStream platformSpecificSources(String relativePathPrefix, String src) throws IOException {
    return new PixiResourceInputStream(relativePathPrefix, src);
  }

  @Override
  public ResourceBitmap renderSvg(InputStream inputStream, float scaleFactor, int width, int height, int percent, int hash) throws IOException {
    throw new UnsupportedOperationException();
  }
}

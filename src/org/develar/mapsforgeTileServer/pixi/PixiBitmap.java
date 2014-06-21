package org.develar.mapsforgeTileServer.pixi;

import org.mapsforge.core.graphics.TileBitmap;

import java.io.IOException;
import java.io.OutputStream;

public class PixiBitmap extends DrawPath implements TileBitmap {
  private final int width;
  private final int height;

  public PixiBitmap(int width, int height) {
    this.width = width;
    this.height = height;
  }

  @Override
  public void compress(OutputStream outputStream) throws IOException {
  }

  @Override
  public int getHeight() {
    return height;
  }

  @Override
  public int getWidth() {
    return width;
  }

  @Override
  public void incrementRefCount() {
  }

  @Override
  public void decrementRefCount() {
  }

  @Override
  public void scaleTo(int width, int height) {
  }

  @Override
  public void setBackgroundColor(int color) {
    throw new UnsupportedOperationException();
  }
}

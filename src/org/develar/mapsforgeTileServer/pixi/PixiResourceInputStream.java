package org.develar.mapsforgeTileServer.pixi;

import java.io.IOException;
import java.io.InputStream;

public class PixiResourceInputStream extends InputStream {
  public PixiResourceInputStream(String relativePathPrefix, String src) {
  }

  @Override
  public int read() throws IOException {
    throw new UnsupportedOperationException();
  }
}

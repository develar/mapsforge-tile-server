package org.mapsforge.map.awt;

import org.jetbrains.annotations.NotNull;
import org.mapsforge.core.graphics.Bitmap;

import java.awt.image.BufferedImage;

// todo fix mapsforge AwtGraphicFactory - expose bufferedImage
public class AwtGraphicFactoryUtil {
  public static BufferedImage getBufferedImage(@NotNull Bitmap bitmap) {
    return ((AwtBitmap)bitmap).bufferedImage;
  }
}

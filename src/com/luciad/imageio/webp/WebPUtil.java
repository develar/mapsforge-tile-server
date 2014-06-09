package com.luciad.imageio.webp;

import org.jetbrains.annotations.NotNull;

import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.Locale;

public final class WebPUtil {
  private static final WebPWriteParam WRITE_PARAM = new WebPWriteParam(Locale.ENGLISH);

  static {
    WRITE_PARAM.setCompressionType("Lossless");
  }

  @NotNull
  public static byte[] encode(@NotNull RenderedImage image) throws IOException {
    return WebP.encode(WRITE_PARAM, image);
  }
}

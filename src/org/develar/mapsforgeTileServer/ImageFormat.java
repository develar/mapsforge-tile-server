package org.develar.mapsforgeTileServer;

import com.google.common.base.Strings;
import org.jetbrains.annotations.Nullable;

public enum ImageFormat {
  WEBP, PNG;

  String getContentType() {
    return this == WEBP ? "image/webp" : "image/png";
  }

  String getFormatName() {
    return this == WEBP ? "webp" : "png";
  }

  @Nullable
  public static ImageFormat fromName(@Nullable String name) {
    if (Strings.isNullOrEmpty(name)) {
      return null;
    }
    else {
      try {
        return valueOf(name);
      }
      catch (IllegalArgumentException e) {
        return null;
      }
    }
  }
}

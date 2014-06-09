package org.develar.mapsforgeTileServer;

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
    if (name == null || name.isEmpty()) {
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

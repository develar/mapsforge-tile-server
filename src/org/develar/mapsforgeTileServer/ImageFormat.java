package org.develar.mapsforgeTileServer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum ImageFormat {
  WEBP, PNG, VECTOR;

  @NotNull
  String getContentType() {
    switch (this) {
      case WEBP:
        return "image/webp";
      case PNG:
        return "image/png";
      case VECTOR:
        return "application/octet-stream";

      default:
        throw new IllegalStateException();
    }
  }

  @SuppressWarnings("UnusedDeclaration")
  String getFormatName() {
    return this == WEBP ? "webp" : this == PNG ? "png" : "pixi";
  }

  @Nullable
  public static ImageFormat fromName(@Nullable String name) {
    if (name == null || name.isEmpty()) {
      return null;
    }
    else {
      return name.charAt(0) == 'w' ? WEBP : (name.charAt(0) == 'p' ? PNG : VECTOR);
    }
  }
}

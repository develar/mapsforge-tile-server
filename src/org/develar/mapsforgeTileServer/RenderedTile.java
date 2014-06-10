package org.develar.mapsforgeTileServer;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

public final class RenderedTile implements Serializable {
  public final byte[] data;

  public final long lastModified;
  public final String etag;

  public RenderedTile(@NotNull byte[] data, long lastModified, @NotNull String etag) {
    this.data = data;
    this.lastModified = lastModified;
    this.etag = etag;
  }

  public int computeWeight() {
    return data.length + 8 + etag.length();
  }
}

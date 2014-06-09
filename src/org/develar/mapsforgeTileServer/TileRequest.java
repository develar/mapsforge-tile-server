package org.develar.mapsforgeTileServer;

import org.jetbrains.annotations.NotNull;
import org.mapsforge.core.model.Tile;

import java.io.Serializable;

class TileRequest extends Tile implements Serializable {
  public final int flags;

  public TileRequest(long tileX, long tileY, byte zoomLevel, int flags) {
    super(tileX, tileY, zoomLevel);

    this.flags = flags;
  }

  public TileRequest(long tileX, long tileY, byte zoomLevel, @NotNull ImageFormat imageFormat) {
    this(tileX, tileY, zoomLevel, imageFormat.ordinal());
  }

  @Override
  public int hashCode() {
    return super.hashCode() + flags;
  }

  @Override
  public boolean equals(Object object) {
    return super.equals(object) && ((TileRequest)object).flags == flags;
  }
}

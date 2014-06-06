package develar.mapsforgeTileServer;

import org.jetbrains.annotations.NotNull;
import org.mapsforge.core.model.Tile;

class TileEx extends Tile {
  private final int flags;

  public TileEx(long tileX, long tileY, byte zoomLevel, @NotNull ImageFormat imageFormat) {
    super(tileX, tileY, zoomLevel);

    flags = imageFormat.ordinal();
  }

  @Override
  public int hashCode() {
    return super.hashCode() + flags;
  }

  @Override
  public boolean equals(Object object) {
    return super.equals(object) && ((TileEx)object).flags == flags;
  }
}

package org.develar.mapsforgeTileServer;

import org.jetbrains.annotations.NotNull;
import org.mapdb.DataInput2;
import org.mapdb.DataOutput2;
import org.mapdb.Serializer;
import org.mapsforge.core.model.Tile;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;

final class TileRequest extends Tile implements Serializable {
  private static final int DEFAULT_TILE_SIZE = 256;

  public static final int WEIGHT = 8 + 8 + 1 + 1;

  private final byte imageFormat;

  public TileRequest(long tileX, long tileY, byte zoomLevel, byte imageFormat) {
    super(tileX, tileY, zoomLevel, DEFAULT_TILE_SIZE);

    this.imageFormat = imageFormat;
  }

  @Override
  protected Tile createNeighbourTile(long y, long x) {
    return new TileRequest(x, y, zoomLevel, imageFormat);
  }

  @NotNull
  public ImageFormat getImageFormat() {
    return imageFormat == 0 ? ImageFormat.WEBP : ImageFormat.PNG;
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode() + imageFormat;
  }

  @Override
  public boolean equals(Object object) {
    return super.equals(object) && ((TileRequest)object).imageFormat == imageFormat;
  }

  public static final class TileRequestSerializer implements Serializer<TileRequest>, Serializable {
    @Override
    public void serialize(DataOutput out, TileRequest value) throws IOException {
      DataOutput2.packLong(out, value.tileX);
      DataOutput2.packLong(out, value.tileY);
      out.write(value.zoomLevel);
      out.write(value.imageFormat);
    }

    @Override
    public TileRequest deserialize(DataInput in, int available) throws IOException {
      return new TileRequest(DataInput2.unpackLong(in), DataInput2.unpackLong(in), in.readByte(), in.readByte());
    }

    @Override
    public int fixedSize() {
      return -1;
    }
  }
}

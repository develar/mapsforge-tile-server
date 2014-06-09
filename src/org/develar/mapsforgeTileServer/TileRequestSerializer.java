package org.develar.mapsforgeTileServer;

import org.mapdb.DataInput2;
import org.mapdb.DataOutput2;
import org.mapdb.Serializer;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;

public final class TileRequestSerializer implements Serializer<TileRequest>, Serializable {
  @Override
  public void serialize(DataOutput out, TileRequest value) throws IOException {
    DataOutput2.packLong(out, value.tileX);
    DataOutput2.packLong(out, value.tileY);
    out.write(value.zoomLevel);
    DataOutput2.packInt(out, value.flags);
  }

  @Override
  public TileRequest deserialize(DataInput in, int available) throws IOException {
    return new TileRequest(DataInput2.unpackLong(in), DataInput2.unpackLong(in), in.readByte(), DataInput2.unpackInt(in));
  }

  @Override
  public int fixedSize() {
    return 0;
  }
}

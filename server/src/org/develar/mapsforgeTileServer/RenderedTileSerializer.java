package org.develar.mapsforgeTileServer;

import org.mapdb.DataInput2;
import org.mapdb.DataOutput2;
import org.mapdb.Serializer;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;

public final class RenderedTileSerializer implements Serializer<RenderedTile>, Serializable {
  @Override
  public void serialize(DataOutput out, RenderedTile value) throws IOException {
    DataOutput2.packLong(out, value.lastModified);
    Serializer.STRING_ASCII.serialize(out, value.etag);
    Serializer.BYTE_ARRAY.serialize(out, value.data);
  }

  @Override
  public RenderedTile deserialize(DataInput in, int available) throws IOException {
    long lastModified = DataInput2.unpackLong(in);
    String etag = Serializer.STRING_ASCII.deserialize(in, available);
    byte[] data = Serializer.BYTE_ARRAY.deserialize(in, available);
    return new RenderedTile(data, lastModified, etag);
  }

  @Override
  public int fixedSize() {
    return -1;
  }
}

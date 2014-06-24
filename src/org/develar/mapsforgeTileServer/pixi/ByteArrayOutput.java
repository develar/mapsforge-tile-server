package org.develar.mapsforgeTileServer.pixi;

import org.jetbrains.annotations.NotNull;

import java.io.OutputStream;
import java.util.Arrays;

public final class ByteArrayOutput extends OutputStream {
  protected byte buffer[];
  protected int count;

  public ByteArrayOutput() {
    this(32);
  }

  public ByteArrayOutput(int size) {
    if (size < 0) {
      throw new IllegalArgumentException("Negative initial size: "
        + size);
    }
    buffer = new byte[size];
  }

  private void ensureCapacity(int minCapacity) {
    if (minCapacity - buffer.length > 0) {
      grow(minCapacity);
    }
  }

  private void grow(int minCapacity) {
    // overflow-conscious code
    int oldCapacity = buffer.length;
    int newCapacity = oldCapacity << 1;
    if (newCapacity - minCapacity < 0) {
      newCapacity = minCapacity;
    }
    if (newCapacity < 0) {
      if (minCapacity < 0) // overflow
      {
        throw new OutOfMemoryError();
      }
      newCapacity = Integer.MAX_VALUE;
    }
    buffer = Arrays.copyOf(buffer, newCapacity);
  }

  public void write(int b) {
    ensureCapacity(count + 1);
    buffer[count++] = (byte)b;
  }

  public void write(@NotNull byte b[], int off, int len) {
    if ((off < 0) || (off > b.length) || (len < 0) ||
      ((off + len) - b.length > 0)) {
      throw new IndexOutOfBoundsException();
    }
    ensureCapacity(count + len);
    System.arraycopy(b, off, buffer, count, len);
    count += len;
  }

  public void writeTo(ByteArrayOutput out) {
    out.write(buffer, 0, count);
  }

  public void reset() {
    count = 0;
  }

  public byte[] toByteArray() {
    if (buffer.length == count) {
      return buffer;
    }
    else {
      return Arrays.copyOf(buffer, count);
    }
  }

  public int size() {
    return count;
  }

  public void close() {
  }

  public void writeShort(int v, int offset) {
    buffer[offset] = (byte)((v >>> 8) & 0xFF);
    buffer[offset + 1] = (byte)(v & 0xFF);
  }

  public int allocateShort() {
    int c = count;
    buffer[count++] = 0;
    buffer[count++] = 0;
    return c;
  }

  public final void writeUnsighedInt29(int v) {
    if (v < 0x80) {
      if (v < 0) {
        throw new IllegalArgumentException("Integer out of range: " + v);
      }
      write(v);
    }
    else if (v < 0x4000) {
      ensureCapacity(count + 2);
      buffer[count++] = (byte)(((v >> 7) & 0x7F) | 0x80);
      buffer[count++] = (byte)(v & 0x7F);
    }
    else if (v < 0x200000) {
      ensureCapacity(count + 3);
      buffer[count++] = (byte)(((v >> 14) & 0x7F) | 0x80);
      buffer[count++] = (byte)(((v >> 7) & 0x7F) | 0x80);
      buffer[count++] = (byte)(v & 0x7F);
    }
    else if (v < 0x40000000) {
      ensureCapacity(count + 4);
      buffer[count++] = (byte)(((v >> 22) & 0x7F) | 0x80);
      buffer[count++] = (byte)(((v >> 15) & 0x7F) | 0x80);
      buffer[count++] = (byte)(((v >> 8) & 0x7F) | 0x80);
      buffer[count++] = (byte)(v & 0xFF);
    }
    else {
      throw new IllegalArgumentException("Integer out of range: " + v);
    }
  }

  public void writeInt(int v) {
    ensureCapacity(count + 4);
    buffer[count++] = (byte)((v >>> 24) & 0xFF);
    buffer[count++] = (byte)((v >>> 16) & 0xFF);
    buffer[count++] = (byte)((v >>> 8) & 0xFF);
    buffer[count++] = (byte)((v) & 0xFF);
  }

  public void writeSignedVarInt(int v) {
    writeUnsighedInt29((v << 1) ^ (v >> 31));
  }
}

package org.develar.mapsforgeTileServer.pixi

import java.io.OutputStream
import java.util.Arrays

class ByteArrayOutput(size: Int = 32) : OutputStream() {
  protected var buffer: ByteArray = ByteArray(size)
  protected var count: Int = 0

  private fun ensureCapacity(minCapacity: Int) {
    if (minCapacity - buffer.size > 0) {
      grow(minCapacity)
    }
  }

  private fun grow(minCapacity: Int) {
    // overflow-conscious code
    val oldCapacity = buffer.size
    var newCapacity = oldCapacity shl 1
    if (newCapacity - minCapacity < 0) {
      newCapacity = minCapacity
    }
    if (newCapacity < 0) {
      if (minCapacity < 0) {
        // overflow
        throw OutOfMemoryError()
      }
      newCapacity = Integer.MAX_VALUE
    }
    buffer = Arrays.copyOf(buffer, newCapacity)
  }

  override fun write(b: Int) {
    ensureCapacity(count + 1)
    buffer[count++] = b.toByte()
  }

  override fun write(b: ByteArray, off: Int, len: Int) {
    if ((off < 0) || (off > b.size) || (len < 0) || ((off + len) - b.size > 0)) {
      throw IndexOutOfBoundsException()
    }
    ensureCapacity(count + len)
    System.arraycopy(b, off, buffer, count, len)
    count += len
  }

  public fun writeTo(out: ByteArrayOutput) {
    out.write(buffer, 0, count)
  }

  public fun reset() {
    count = 0
  }

  public fun toByteArray(): ByteArray {
    if (buffer.size == count) {
      return buffer
    }
    else {
      return Arrays.copyOf(buffer, count)
    }
  }

  public fun size(): Int {
    return count
  }

  override fun close() {
  }

  public fun writeShort(v: Int, offset: Int) {
    buffer[offset] = ((v.ushr(8)) and 255).toByte()
    buffer[offset + 1] = (v and 255).toByte()
  }

  public fun allocateShort(): Int {
    val c = count
    buffer[count++] = 0
    buffer[count++] = 0
    return c
  }

  public fun writeUnsighedVarInt(v: Int) {
    if (v < 128) {
      if (v < 0) {
        throw IllegalArgumentException("Integer out of range: " + v)
      }
      write(v)
    }
    else
      if (v < 16384) {
        ensureCapacity(count + 2)
        buffer[count++] = (((v shr 7) and 127) or 128).toByte()
        buffer[count++] = (v and 127).toByte()
      }
      else
        if (v < 2097152) {
          ensureCapacity(count + 3)
          buffer[count++] = (((v shr 14) and 127) or 128).toByte()
          buffer[count++] = (((v shr 7) and 127) or 128).toByte()
          buffer[count++] = (v and 127).toByte()
        }
        else
          if (v < 1073741824) {
            ensureCapacity(count + 4)
            buffer[count++] = (((v shr 22) and 127) or 128).toByte()
            buffer[count++] = (((v shr 15) and 127) or 128).toByte()
            buffer[count++] = (((v shr 8) and 127) or 128).toByte()
            buffer[count++] = (v and 255).toByte()
          }
          else {
            throw IllegalArgumentException("Integer out of range: " + v)
          }
  }

  public fun writeInt(v: Int) {
    ensureCapacity(count + 4)
    buffer[count++] = ((v.ushr(24)) and 255).toByte()
    buffer[count++] = ((v.ushr(16)) and 255).toByte()
    buffer[count++] = ((v.ushr(8)) and 255).toByte()
    buffer[count++] = ((v) and 255).toByte()
  }

  public fun writeString(s: CharSequence) {
    var utfLen = 0
    for (i in 0..s.length - 1) {
      var c = s.charAt(i).toInt()
      if (c >= 1 && c <= 127) {
        utfLen++
      }
      else
        if (c > 2047) {
          utfLen += 3
        }
        else {
          utfLen += 2
        }
    }

    writeUnsighedVarInt(s.length)

    ensureCapacity(count + utfLen)
    var count = this.count
    for (i in 0..s.length - 1) {
      var c = s.charAt(i).toInt()
      if (c <= 127) {
        buffer[count++] = c.toByte()
      }
      else
        if (c > 2047) {
          buffer[count++] = (224 or ((c shr 12) and 15)).toByte()
          buffer[count++] = (128 or ((c shr 6) and 63)).toByte()
          buffer[count++] = (128 or (c and 63)).toByte()
        }
        else {
          buffer[count++] = (192 or ((c shr 6) and 31)).toByte()
          buffer[count++] = (128 or (c and 63)).toByte()
        }
    }

    this.count = count
  }

  public fun writeSignedVarInt(v: Int) {
    writeUnsighedVarInt((v shl 1) xor (v shr 31))
  }
}

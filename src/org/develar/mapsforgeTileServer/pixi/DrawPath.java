package org.develar.mapsforgeTileServer.pixi;

import org.jetbrains.annotations.NotNull;

public class DrawPath {
  private static final int TWIP_SIZE = 20;

  final ByteArrayOutput out = new ByteArrayOutput();

  protected void beginFill(int color) {
    if (((color >> 24) & 0xff) == 255) {
      writeCommand(PixiCommand.BEGIN_FILL_RGB);
      out.write((color >>> 16) & 0xff);
      out.write((color >>> 8) & 0xff);
      out.write((color) & 0xff);
    }
    else {
      writeCommand(PixiCommand.BEGIN_FILL_RGBA, color);
    }
  }

  protected void moveToOrLineTo(PixiCommand command, float x, float y) {
    writeCommand(command);
    writeAsTwips(x);
    writeAsTwips(y);
  }

  protected void moveToOrLineTo(PixiCommand command, int x, int y) {
    writeCommand(command);
    writeAsTwips(x);
    writeAsTwips(y);
  }

  protected void writeAsTwips(int v) {
    out.writeSignedVarInt(v * TWIP_SIZE);
  }

  // zig-zag encoding
  // http://code.google.com/apis/protocolbuffers/docs/encoding.html#types
  protected void writeAsTwips(float v) {
    out.writeSignedVarInt(Math.round(v * TWIP_SIZE));
  }

  protected void drawCircle(PixiCommand command, int x, int y, int radius) {
    out.write(command.ordinal());
    out.writeUnsighedInt29(x);
    out.writeUnsighedInt29(y);
    out.writeUnsighedInt29(radius);
  }

  protected void writeCommand(PixiCommand command, int color) {
    out.write(command.ordinal());
    out.writeInt(color);
  }

  protected void writeCommand(PixiCommand command) {
    out.write(command.ordinal());
  }

  public void writePath(PixiPath path) {
    path.closePolyline();
    path.out.writeTo(out);
  }

  @NotNull
  public byte[] build() {
    return out.toByteArray();
  }
}

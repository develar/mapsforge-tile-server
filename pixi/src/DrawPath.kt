package org.develar.mapsforgeTileServer.pixi

open class DrawPath() {
  class object {
    private val TWIP_SIZE = 20
  }

  val out = ByteArrayOutput()

  protected fun beginFill(color: Int) {
    if (((color shr 24) and 255) == 255) {
      writeCommand(PixiCommand.BEGIN_FILL_RGB)
      out.write((color.ushr(16)) and 255)
      out.write((color.ushr(8)) and 255)
      out.write((color) and 255)
    }
    else {
      writeCommand(PixiCommand.BEGIN_FILL_RGBA, color)
    }
  }

  public fun moveToOrLineTo(command: PixiCommand, x: Float, y: Float) {
    writeCommand(command)
    writeAsTwips(x)
    writeAsTwips(y)
  }

  public fun moveToOrLineTo(command: PixiCommand, x: Int, y: Int) {
    writeCommand(command)
    writeAsTwips(x)
    writeAsTwips(y)
  }

  protected fun writeAsTwips(v: Int) {
    out.writeSignedVarInt(v * TWIP_SIZE)
  }

  // zig-zag encoding
  // http://code.google.com/apis/protocolbuffers/docs/encoding.html#types
  public fun writeAsTwips(v: Float) {
    out.writeSignedVarInt(Math.round(v * TWIP_SIZE.toFloat()))
  }

  public fun writeAsTwips(v: Double) {
    out.writeSignedVarInt(Math.round(v * TWIP_SIZE.toDouble()).toInt())
  }

  public fun drawCircle(command: PixiCommand, x: Int, y: Int, radius: Int) {
    out.write(command.ordinal())
    out.writeSignedVarInt(x)
    out.writeSignedVarInt(y)
    out.writeSignedVarInt(radius)
  }

  protected fun writeCommand(command: PixiCommand, color: Int) {
    out.write(command.ordinal())
    out.writeInt(color)
  }

  public fun writeCommand(command: PixiCommand) {
    out.write(command.ordinal())
  }

  public fun writePath(path: PixiPath) {
    path.closePolyline()
    path.out.writeTo(out)
  }

  public fun build(): ByteArray = out.toByteArray()
}

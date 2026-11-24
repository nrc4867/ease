package dev.chieppa.ease.net.input

import dev.chieppa.ease.ecs.replication.BitReader
import dev.chieppa.ease.ecs.replication.BitWriter
import dev.chieppa.ease.ecs.replication.DecodeStatus
import dev.chieppa.ease.ecs.replication.WireBits
import dev.chieppa.ease.net.channel.Channels

/**
 * One input message from a client, decoded into reused frames. On the wire: the newest snapshot
 * tick the client applied plus one (0 when none) and 32 bits for the 32 ticks before it, as a
 * variable-length number and an `Int`; the tick of the newest input frame; the frame count less one
 * in three bits; then the frames, newest first, each for the tick before the one ahead of it. So
 * each packet repeats the last few ticks, and one lost packet never loses a tick.
 */
internal class InputPacket(private val layout: InputLayout) {
  /** The newest snapshot tick the client applied, or -1. */
  var ackTick: Long = -1L

  /** Bit `i` set: the client applied the snapshot of `ackTick - 1 - i` too. */
  var ackEarlier: Int = 0

  /** The tick of [frames]' first entry. */
  var newestTick: Long = 0L

  /** How many of [frames] the packet holds, from 1 to [MAX_FRAMES]. */
  var count: Int = 0

  /** The frames, newest first. */
  val frames: Array<InputFrame> = Array(MAX_FRAMES) { InputFrame(layout) }

  private val reader = BitReader()

  /** Writes the packet into [writer], which it empties, and returns its length in bytes. */
  fun write(writer: BitWriter): Int {
    writer.reset()
    writer.writeVarLong(ackTick + 1)
    writer.writeBits(ackEarlier, Int.SIZE_BITS)
    writer.writeVarLong(newestTick)
    writer.writeBits(count - 1, COUNT_BITS)
    for (index in 0 until count) writeFrame(writer, frames[index])
    writer.padToByte()
    return writer.byteLength
  }

  /**
   * Reads the first [length] bytes of [bytes] into this packet, treating them as hostile, and
   * returns [DecodeStatus.OK] or why they were refused; a refused packet leaves partial values.
   */
  fun read(bytes: ByteArray, length: Int): DecodeStatus =
    when {
      length < 0 || length > bytes.size -> DecodeStatus.TRUNCATED
      length > Channels.MAX_UNRELIABLE_BYTES -> DecodeStatus.TOO_LONG
      else -> readChecked(bytes, length)
    }

  private fun readChecked(bytes: ByteArray, length: Int): DecodeStatus {
    reader.reset(bytes, length)
    val ackPlusOne = reader.readVarLong()
    ackEarlier = reader.readBits(Int.SIZE_BITS)
    newestTick = reader.readVarLong()
    count = reader.readBits(COUNT_BITS) + 1
    if (reader.ok && (ackPlusOne < 0 || newestTick < count - 1)) reader.fail(DecodeStatus.MALFORMED)
    ackTick = ackPlusOne - 1
    var index = 0
    while (reader.ok && index < count) readFrame(frames[index++])
    if (reader.ok && reader.remainingBits >= WireBits.BYTE_BITS)
      reader.fail(DecodeStatus.TRAILING_DATA)
    return reader.status
  }

  private fun writeFrame(writer: BitWriter, frame: InputFrame) {
    for (axis in 0 until layout.axes) writer.writeBits(frame.axisStep(axis), InputLayout.AXIS_BITS)
    writer.writeBits(frame.buttons, layout.buttons)
  }

  private fun readFrame(frame: InputFrame) {
    for (axis in 0 until layout.axes) {
      val step = reader.readBits(InputLayout.AXIS_BITS)
      if (step > InputFrame.MAX_STEP) reader.fail(DecodeStatus.BAD_VALUE)
      if (reader.ok) frame.setAxisStep(axis, step)
    }
    frame.setButtonBits(reader.readBits(layout.buttons))
  }

  companion object {
    /** The most ticks of input one packet repeats. */
    const val MAX_FRAMES: Int = 8

    /** Bits the frame count takes. */
    const val COUNT_BITS: Int = 3
  }
}

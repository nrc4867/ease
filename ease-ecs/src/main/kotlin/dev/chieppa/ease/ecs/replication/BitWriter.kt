package dev.chieppa.ease.ecs.replication

/**
 * Writes values bit by bit into [buffer], lowest bits first, without allocating. Writing past the
 * buffer's end throws [BitWriterFull]: the engine sizes its own packets, so that is a bug, not
 * hostile input.
 */
public class BitWriter(capacityBytes: Int) {
  /** The bytes written so far are its first [byteLength]. */
  public val buffer: ByteArray = ByteArray(capacityBytes)

  /** How many bits have been written. */
  public var bitPosition: Int = 0
    private set

  /** How many bytes the bits written so far take, the last one padded with zeros. */
  public val byteLength: Int
    get() = WireBits.bytesFor(bitPosition)

  /** Starts again from an empty buffer. */
  public fun reset() {
    bitPosition = 0
  }

  /** Writes the lowest [count] bits of [value], for [count] from 0 to 32. */
  public fun writeBits(value: Int, count: Int) {
    require(count in 0..WireBits.INT_BITS) { "Can write 0 to 32 bits at once, not $count" }
    if (bitPosition + count > buffer.size * WireBits.BYTE_BITS) {
      throw BitWriterFull(
        "A ${buffer.size}-byte buffer has no room for $count more bits at bit $bitPosition"
      )
    }
    var remaining = count
    var bits = value
    while (remaining > 0) {
      val offset = bitPosition and WireBits.BYTE_MASK
      val taken = minOf(WireBits.BYTE_BITS - offset, remaining)
      putBits(bitPosition ushr WireBits.BYTE_SHIFT, offset, bits, taken)
      bits = bits ushr taken
      remaining -= taken
      bitPosition += taken
    }
  }

  /** Overwrites [count] bits at [position], already written, with the lowest bits of [value]. */
  public fun writeBitsAt(position: Int, value: Int, count: Int) {
    require(position >= 0 && position + count <= bitPosition) {
      "Bits $position to ${position + count} have not been written yet"
    }
    val end = bitPosition
    bitPosition = position
    writeBits(value, count)
    bitPosition = end
  }

  /** Moves back to [position], already written, so the bits after it are written again. */
  public fun rewind(position: Int) {
    require(position in 0..bitPosition) { "Bit $position has not been written yet" }
    bitPosition = position
  }

  /** Writes [value] as one bit. */
  public fun writeBoolean(value: Boolean) {
    writeBits(if (value) 1 else 0, 1)
  }

  /** Writes [value], which must not be negative, in groups of seven bits, small values shortest. */
  public fun writeVarLong(value: Long) {
    require(value >= 0) { "Variable-length numbers are not negative: $value" }
    var rest = value
    while (true) {
      val group = (rest and WireBits.VARINT_GROUP.toLong()).toInt()
      rest = rest ushr WireBits.VARINT_GROUP_BITS
      writeBits(group, WireBits.VARINT_GROUP_BITS)
      writeBoolean(rest != 0L)
      if (rest == 0L) return
    }
  }

  /** Pads the last byte with zeros, so [byteLength] bytes of [buffer] hold the whole message. */
  public fun padToByte() {
    val padding = (WireBits.BYTE_BITS - (bitPosition and WireBits.BYTE_MASK)) and WireBits.BYTE_MASK
    writeBits(0, padding)
  }

  private fun putBits(index: Int, offset: Int, bits: Int, count: Int) {
    val mask = ((1 shl count) - 1) shl offset
    val kept = buffer[index].toInt() and mask.inv()
    buffer[index] = (kept or ((bits shl offset) and mask)).toByte()
  }
}

package dev.chieppa.ease.ecs.replication

private const val BYTE_BITS = 8
private const val BYTE_MASK = 7
private const val BYTE_SHIFT = 3
private const val INT_BITS = 32
private const val VARINT_GROUP_BITS = 7
private const val VARINT_GROUP = 0x7F

/**
 * Writes values bit by bit into [buffer], lowest bits first, without allocating. Writing past the
 * buffer's end throws: the engine sizes its own packets, so that is a bug, not hostile input.
 */
public class BitWriter(capacityBytes: Int) {
  /** The bytes written so far are its first [byteLength]. */
  public val buffer: ByteArray = ByteArray(capacityBytes)

  /** How many bits have been written. */
  public var bitPosition: Int = 0
    private set

  /** How many bytes the bits written so far take, the last one padded with zeros. */
  public val byteLength: Int
    get() = (bitPosition + BYTE_MASK) ushr BYTE_SHIFT

  /** Starts again from an empty buffer. */
  public fun reset() {
    bitPosition = 0
  }

  /** Writes the lowest [count] bits of [value], for [count] from 0 to 32. */
  public fun writeBits(value: Int, count: Int) {
    require(count in 0..INT_BITS) { "Can write 0 to 32 bits at once, not $count" }
    check(bitPosition + count <= buffer.size * BYTE_BITS) {
      "A ${buffer.size}-byte buffer has no room for $count more bits at bit $bitPosition"
    }
    var remaining = count
    var bits = value
    while (remaining > 0) {
      val offset = bitPosition and BYTE_MASK
      val taken = minOf(BYTE_BITS - offset, remaining)
      putBits(bitPosition ushr BYTE_SHIFT, offset, bits, taken)
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

  /** Writes [value] as one bit. */
  public fun writeBoolean(value: Boolean) {
    writeBits(if (value) 1 else 0, 1)
  }

  /** Writes [value], which must not be negative, in groups of seven bits, small values shortest. */
  public fun writeVarLong(value: Long) {
    require(value >= 0) { "Variable-length numbers are not negative: $value" }
    var rest = value
    while (true) {
      val group = (rest and VARINT_GROUP.toLong()).toInt()
      rest = rest ushr VARINT_GROUP_BITS
      writeBits(group, VARINT_GROUP_BITS)
      writeBoolean(rest != 0L)
      if (rest == 0L) return
    }
  }

  /** Pads the last byte with zeros, so [byteLength] bytes of [buffer] hold the whole message. */
  public fun padToByte() {
    val padding = (BYTE_BITS - (bitPosition and BYTE_MASK)) and BYTE_MASK
    writeBits(0, padding)
  }

  private fun putBits(index: Int, offset: Int, bits: Int, count: Int) {
    val mask = ((1 shl count) - 1) shl offset
    val kept = buffer[index].toInt() and mask.inv()
    buffer[index] = (kept or ((bits shl offset) and mask)).toByte()
  }
}

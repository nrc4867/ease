package dev.chieppa.ease.textures.bc

/** Bits in a BC7 block. */
private const val BLOCK_BITS = 128

/** Bytes in a BC7 block. */
internal const val BC7_BLOCK_BYTES: Int = 16

private const val BYTE_BITS = 8
private const val BYTE_MASK = 0xFF

/**
 * A cursor over the 128 bits of one block, least significant bit of byte 0 first, as BC7 lays its
 * fields out. Reused from block to block.
 */
internal class Bc7Bits {
  private val bits = IntArray(BLOCK_BITS)
  private var position = 0

  /** Starts reading the block at [offset] of [bytes]. */
  fun load(bytes: ByteArray, offset: Int) {
    for (bit in 0 until BLOCK_BITS) {
      val byte = bytes[offset + bit / BYTE_BITS].toInt() and BYTE_MASK
      bits[bit] = (byte shr (bit % BYTE_BITS)) and 1
    }
    position = 0
  }

  /** Starts writing a block from its first bit. */
  fun clear() {
    bits.fill(0)
    position = 0
  }

  /** The next [count] bits as a number, the first one lowest. */
  fun read(count: Int): Int {
    var value = 0
    for (bit in 0 until count) value = value or (bits[position + bit] shl bit)
    position += count
    return value
  }

  /** Appends the low [count] bits of [value], lowest first. */
  fun write(value: Int, count: Int) {
    for (bit in 0 until count) bits[position + bit] = (value shr bit) and 1
    position += count
  }

  /** Stores the block written so far at [offset] of [bytes]; every one of its bits must be set. */
  fun store(bytes: ByteArray, offset: Int) {
    check(position == BLOCK_BITS) { "A BC7 block holds $BLOCK_BITS bits, not $position" }
    for (byte in 0 until BC7_BLOCK_BYTES) {
      var value = 0
      for (bit in 0 until BYTE_BITS) value = value or (bits[byte * BYTE_BITS + bit] shl bit)
      bytes[offset + byte] = value.toByte()
    }
  }
}

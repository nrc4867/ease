package dev.chieppa.ease.ecs.replication

/** The bit widths and masks the replication wire form is built from. */
public object WireBits {
  /** Bits in a byte. */
  public const val BYTE_BITS: Int = 8

  /** Masks a bit position to its offset inside a byte. */
  public const val BYTE_MASK: Int = 7

  /** Shifts a bit position to its byte index. */
  public const val BYTE_SHIFT: Int = 3

  /** Masks a byte to its unsigned value. */
  public const val BYTE_VALUE: Int = 0xFF

  /** Bits in an `Int`. */
  public const val INT_BITS: Int = 32

  /** Bits in a full-precision `Float`. */
  public const val FLOAT_BITS: Int = 32

  /** Value bits in each group of a variable-length number; a continuation bit follows each. */
  public const val VARINT_GROUP_BITS: Int = 7

  /** Masks a variable-length number's lowest group. */
  public const val VARINT_GROUP: Int = 0x7F

  /** The most groups a variable-length number may take: enough for any `Long`. */
  public const val VARINT_MAX_GROUPS: Int = 10

  /** The bytes [bits] bits take, the last one padded. */
  public fun bytesFor(bits: Int): Int = (bits + BYTE_MASK) ushr BYTE_SHIFT
}

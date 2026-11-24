package dev.chieppa.ease.ecs.replication

/** The bit widths and masks the replication wire form is built from. */
internal object WireBits {
  /** Bits in a byte. */
  const val BYTE_BITS = 8

  /** Masks a bit position to its offset inside a byte. */
  const val BYTE_MASK = 7

  /** Shifts a bit position to its byte index. */
  const val BYTE_SHIFT = 3

  /** Masks a byte to its unsigned value. */
  const val BYTE_VALUE = 0xFF

  /** Bits in an `Int`. */
  const val INT_BITS = 32

  /** Bits in a full-precision `Float`. */
  const val FLOAT_BITS = 32

  /** Value bits in each group of a variable-length number; a continuation bit follows each. */
  const val VARINT_GROUP_BITS = 7

  /** Masks a variable-length number's lowest group. */
  const val VARINT_GROUP = 0x7F

  /** The most groups a variable-length number may take: enough for any `Long`. */
  const val VARINT_MAX_GROUPS = 10
}

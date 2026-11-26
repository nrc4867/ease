package dev.chieppa.ease.gltf

import dev.chieppa.ease.memory.MappedRegion

/** glTF's component types: the GL constants for the numbers an accessor's elements are made of. */
internal object ComponentTypes {
  const val BYTE = 5120
  const val UNSIGNED_BYTE = 5121
  const val SHORT = 5122
  const val UNSIGNED_SHORT = 5123
  const val UNSIGNED_INT = 5125
  const val FLOAT = 5126

  private const val BYTE_MAX = 127f
  private const val UNSIGNED_BYTE_MAX = 255f
  private const val SHORT_MAX = 32767f
  private const val UNSIGNED_SHORT_MAX = 65535f
  private const val SHORT_SIGN = 0x8000
  private const val BYTE_SIGN = 0x80
  private const val SHORT_RANGE = 0x10000
  private const val BYTE_RANGE = 0x100

  /** Bytes one number of [type] takes, or null when [type] is not a component type. */
  fun size(type: Int): Int? =
    when (type) {
      BYTE,
      UNSIGNED_BYTE -> 1
      SHORT,
      UNSIGNED_SHORT -> 2
      UNSIGNED_INT,
      FLOAT -> Int.SIZE_BYTES
      else -> null
    }

  /** Whether [type] holds integers. */
  fun isInteger(type: Int): Boolean = type != FLOAT

  /** The number of [type] at [offset] of [region], as its raw value. */
  fun raw(region: MappedRegion, offset: Long, type: Int): Double =
    when (type) {
      BYTE -> signed(region.uint8(offset), BYTE_SIGN, BYTE_RANGE).toDouble()
      UNSIGNED_BYTE -> region.uint8(offset).toDouble()
      SHORT -> signed(region.uint16(offset), SHORT_SIGN, SHORT_RANGE).toDouble()
      UNSIGNED_SHORT -> region.uint16(offset).toDouble()
      UNSIGNED_INT -> region.uint32(offset).toDouble()
      else -> region.float32(offset).toDouble()
    }

  /**
   * The number of [type] at [offset] of [region] as a float: integers mapped to 0 to 1 (or -1 to 1
   * when signed) when [normalized], their plain values otherwise.
   */
  fun float(region: MappedRegion, offset: Long, type: Int, normalized: Boolean): Float {
    val value = raw(region, offset, type).toFloat()
    if (!normalized) return value
    return when (type) {
      BYTE -> maxOf(value / BYTE_MAX, -1f)
      UNSIGNED_BYTE -> value / UNSIGNED_BYTE_MAX
      SHORT -> maxOf(value / SHORT_MAX, -1f)
      UNSIGNED_SHORT -> value / UNSIGNED_SHORT_MAX
      else -> value
    }
  }

  private fun signed(value: Int, sign: Int, range: Int): Int =
    if (value and sign != 0) value - range else value
}

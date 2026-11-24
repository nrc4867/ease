package dev.chieppa.ease.ecs.replication

import kotlin.math.roundToLong

/**
 * How fields go on the wire, shared by the serializers KSP writes and the schema-driven ones.
 * Floats go at full precision or quantized; reading one that is not finite fails as
 * [DecodeStatus.BAD_VALUE].
 */
public object ReplicatedFields {
  /** The most fields a replicated component may have: its change mask is one `Int`. */
  public const val MAX_FIELDS: Int = 32

  /** The fewest and most bits a quantized float may take. */
  public val QUANTIZED_BITS: IntRange = 1..30

  /** The mask with a bit for each of [count] fields. */
  public fun allFields(count: Int): Int = if (count >= MAX_FIELDS) -1 else (1 shl count) - 1

  /** Whether [a] and [b] are the same float bit for bit, so `NaN` equals itself. */
  public fun sameFloat(a: Float, b: Float): Boolean = a.toRawBits() == b.toRawBits()

  /** Writes [value] at full precision. */
  public fun writeFloat(writer: BitWriter, value: Float) {
    writer.writeBits(value.toRawBits(), WireBits.FLOAT_BITS)
  }

  /** Reads a full-precision float, failing on one that is not finite. */
  public fun readFloat(reader: BitReader): Float {
    val value = Float.fromBits(reader.readBits(WireBits.FLOAT_BITS))
    if (value.isFinite()) return value
    reader.fail(DecodeStatus.BAD_VALUE)
    return 0f
  }

  /**
   * [value] as a step from 0 to `2^bits - 1` over [min] to [max], clamped; `NaN` is [min]. The
   * arithmetic is in `Double`, since a `Float` cannot hold `2^bits - 1` past 24 bits.
   */
  public fun quantize(value: Float, bits: Int, min: Float, max: Float): Int {
    val steps = (1L shl bits) - 1
    val clamped = if (value.isNaN()) min else value.coerceIn(min, max)
    val fraction = (clamped.toDouble() - min) / (max.toDouble() - min)
    val step = (fraction * steps).roundToLong()
    return step.coerceIn(0L, steps).toInt()
  }

  /** The float a [quantize]d [step] stands for, computed in `Double` so wide steps stay exact. */
  public fun dequantize(step: Int, bits: Int, min: Float, max: Float): Float {
    val steps = (1L shl bits) - 1
    return (min + (max.toDouble() - min) * step / steps).toFloat()
  }

  /** Writes [value] quantized to [bits] bits over [min] to [max]. */
  public fun writeQuantized(writer: BitWriter, value: Float, bits: Int, min: Float, max: Float) {
    writer.writeBits(quantize(value, bits, min, max), bits)
  }

  /** Reads a float [writeQuantized] wrote; every step it can hold lies in range. */
  public fun readQuantized(reader: BitReader, bits: Int, min: Float, max: Float): Float =
    dequantize(reader.readBits(bits), bits, min, max)

  /** Writes [value] in 32 bits. */
  public fun writeInt(writer: BitWriter, value: Int) {
    writer.writeBits(value, WireBits.INT_BITS)
  }

  /** Reads a 32-bit int. */
  public fun readInt(reader: BitReader): Int = reader.readBits(WireBits.INT_BITS)
}

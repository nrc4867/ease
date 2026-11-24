package dev.chieppa.ease.ecs.replication

/** A float sent in [bits] bits spread evenly over [min] to [max], as `@Quantized` declares one. */
public class Quantization(public val bits: Int, public val min: Float, public val max: Float) {
  init {
    require(bits in ReplicatedFields.QUANTIZED_BITS) { "Quantized bits are 1 to 30, not $bits" }
    require(min.isFinite() && max.isFinite() && max > min) {
      "A quantized range needs finite min < max, not $min to $max"
    }
  }

  /** [value] as the step it is sent as. */
  public fun stepOf(value: Float): Int = ReplicatedFields.quantize(value, bits, min, max)

  /** Writes [value] quantized. */
  public fun write(writer: BitWriter, value: Float) {
    ReplicatedFields.writeQuantized(writer, value, bits, min, max)
  }

  /** Reads a quantized float. */
  public fun read(reader: BitReader): Float = ReplicatedFields.readQuantized(reader, bits, min, max)
}

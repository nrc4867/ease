package dev.chieppa.ease.ecs.replication

/** Declares a schema's fields, checked as the build-time processor checks a class. */
public class SchemaBuilder internal constructor() {
  private val fields = mutableListOf<SchemaField>()

  /** A full-precision float. */
  public fun float(
    name: String,
    interpolated: Boolean = false,
    audience: Audience = Audience.EVERYONE,
  ) {
    add(SchemaField(name, SchemaFieldKind.FLOAT, interpolated, audience))
  }

  /** A float sent in [bits] bits spread evenly over [range], as `@Quantized` and `@Range` say. */
  public fun quantized(
    name: String,
    bits: Int,
    range: ClosedFloatingPointRange<Float>,
    interpolated: Boolean = false,
    audience: Audience = Audience.EVERYONE,
  ) {
    val quantization = Quantization(bits, range.start, range.endInclusive)
    add(SchemaField(name, SchemaFieldKind.QUANTIZED_FLOAT, interpolated, audience, quantization))
  }

  /** A 32-bit int. */
  public fun int(name: String, audience: Audience = Audience.EVERYONE) {
    add(SchemaField(name, SchemaFieldKind.INT, interpolated = false, audience = audience))
  }

  /** A boolean. */
  public fun boolean(name: String, audience: Audience = Audience.EVERYONE) {
    add(SchemaField(name, SchemaFieldKind.BOOLEAN, interpolated = false, audience = audience))
  }

  private fun add(field: SchemaField) {
    require(fields.none { it.name == field.name }) { "Field ${field.name} is declared twice" }
    require(fields.size < ReplicatedFields.MAX_FIELDS) {
      "A replicated component has at most ${ReplicatedFields.MAX_FIELDS} fields"
    }
    fields += field
  }

  internal fun build(): List<SchemaField> = fields.toList()
}

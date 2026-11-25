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

  /**
   * A rotation group: the four float fields [names] of one unit quaternion, interpolated as one
   * (see `Interpolated.rotation`), in full precision or, with [bits], quantized over -1 to 1.
   */
  public fun rotation(group: String, names: List<String>, bits: Int? = null) {
    require(names.size == RotationBlend.COMPONENTS) {
      "Rotation group $group needs ${RotationBlend.COMPONENTS} fields, not ${names.size}"
    }
    val quantization = bits?.let { Quantization(it, -1f, 1f) }
    val kind = if (bits == null) SchemaFieldKind.FLOAT else SchemaFieldKind.QUANTIZED_FLOAT
    names.forEach { add(SchemaField(it, kind, true, Audience.EVERYONE, quantization, group)) }
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

  internal fun build(): List<SchemaField> {
    val groups = fields.filter { it.rotation.isNotEmpty() }.groupBy { it.rotation }
    groups.forEach { (group, members) ->
      require(members.size == RotationBlend.COMPONENTS) {
        "Rotation group $group is declared twice"
      }
    }
    return fields.toList()
  }
}

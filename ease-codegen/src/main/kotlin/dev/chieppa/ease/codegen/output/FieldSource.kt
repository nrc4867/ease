package dev.chieppa.ease.codegen.output

import dev.chieppa.ease.codegen.model.FieldKind
import dev.chieppa.ease.codegen.model.FieldModel

/** The expressions a generated serializer uses for one field, by how the field goes on the wire. */
internal object FieldSource {
  private const val FIELDS = "ReplicatedFields"
  private const val BLEND = "dev.chieppa.ease.ecs.replication.RotationBlend"

  /** Whether the field's wire value differs between `value` and `baseline`. */
  fun wireDiffers(field: FieldModel): String {
    val value = "value.${field.name}"
    val baseline = "baseline.${field.name}"
    return when (field.kind) {
      FieldKind.QUANTIZED_FLOAT -> "${quantized(value, field)} != ${quantized(baseline, field)}"
      else -> differs(field, value, baseline)
    }
  }

  /** Whether the field differs at all, bit for bit, between `a` and `b`. */
  fun exactlyDiffers(field: FieldModel): String =
    differs(field, "a.${field.name}", "b.${field.name}")

  /** The statement writing the field of `value` to `writer`. */
  fun write(field: FieldModel): String {
    val value = "value.${field.name}"
    return when (field.kind) {
      FieldKind.FLOAT -> "$FIELDS.writeFloat(writer, $value)"
      FieldKind.QUANTIZED_FLOAT -> "$FIELDS.writeQuantized(writer, $value, ${range(field)})"
      FieldKind.INT -> "$FIELDS.writeInt(writer, $value)"
      FieldKind.BOOLEAN -> "writer.writeBoolean($value)"
      FieldKind.ENUM -> "$FIELDS.writeInt(writer, $value.ordinal)"
      null -> error("${field.name} cannot replicate")
    }
  }

  /** The expression reading the field from `reader`. */
  fun read(field: FieldModel): String =
    when (field.kind) {
      FieldKind.FLOAT -> "$FIELDS.readFloat(reader)"
      FieldKind.QUANTIZED_FLOAT -> "$FIELDS.readQuantized(reader, ${range(field)})"
      FieldKind.INT -> "$FIELDS.readInt(reader)"
      FieldKind.BOOLEAN -> "reader.readBoolean()"
      FieldKind.ENUM -> "${field.typeName}.entries[$FIELDS.readInt(reader)]"
      null -> error("${field.name} cannot replicate")
    }

  /**
   * The field `amount` of the way from `from` to `to`: blended when it is interpolated, otherwise
   * as `from` holds it.
   */
  fun blend(field: FieldModel): String {
    val from = "from.${field.name}"
    return if (field.interpolated) "$from + (to.${field.name} - $from) * amount" else from
  }

  /**
   * Component [index] of the rotation [group]'s four fields `amount` of the way from `from` to
   * `to`, blended as one quaternion.
   */
  fun rotationBlend(group: List<FieldModel>, index: Int): String {
    val from = group.joinToString { "from.${it.name}" }
    val to = group.joinToString { "to.${it.name}" }
    return "$BLEND.component($index, $from, $to, amount)"
  }

  private fun differs(field: FieldModel, a: String, b: String): String =
    when (field.kind) {
      FieldKind.FLOAT,
      FieldKind.QUANTIZED_FLOAT -> "!$FIELDS.sameFloat($a, $b)"
      else -> "$a != $b"
    }

  private fun quantized(value: String, field: FieldModel): String =
    "$FIELDS.quantize($value, ${range(field)})"

  private fun range(field: FieldModel): String {
    val bits = checkNotNull(field.quantizedBits)
    val range = checkNotNull(field.range)
    return "$bits, ${range.min.toFloat()}f, ${range.max.toFloat()}f"
  }
}

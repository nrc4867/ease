package dev.chieppa.ease.codegen.output

import dev.chieppa.ease.codegen.model.FieldKind
import dev.chieppa.ease.codegen.model.FieldModel

/** The expressions a generated serializer uses for one field, by how the field goes on the wire. */
internal object FieldSource {
  private const val FIELDS = "ReplicatedFields"

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
      null -> error("${field.name} cannot replicate")
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
    val quantization = checkNotNull(field.quantization)
    return "${quantization.bits}, ${quantization.min}f, ${quantization.max}f"
  }
}

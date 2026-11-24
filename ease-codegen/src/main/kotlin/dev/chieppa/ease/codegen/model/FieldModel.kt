package dev.chieppa.ease.codegen.model

/**
 * One replicated property: its name, its Kotlin type as written in source, and the field options
 * its annotations set. Each option reader fills in its own part.
 */
internal data class FieldModel(
  val name: String,
  /** The property's type, qualified, such as `kotlin.Float`. */
  val typeName: String,
  val nullable: Boolean = false,
  val quantization: Quantization? = null,
  val interpolated: Boolean = false,
  val audience: FieldAudience = FieldAudience.EVERYONE,
  /** Whether generated code can set the property: its setter is not private or protected. */
  val settable: Boolean = true,
) {
  /** How the field goes on the wire, or null for a type that cannot replicate. */
  val kind: FieldKind?
    get() =
      when {
        nullable -> null
        typeName == FLOAT && quantization != null -> FieldKind.QUANTIZED_FLOAT
        quantization != null -> null
        typeName == FLOAT -> FieldKind.FLOAT
        typeName == INT -> FieldKind.INT
        typeName == BOOLEAN -> FieldKind.BOOLEAN
        else -> null
      }

  companion object {
    const val FLOAT = "kotlin.Float"
    const val INT = "kotlin.Int"
    const val BOOLEAN = "kotlin.Boolean"
  }
}

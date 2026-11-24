package dev.chieppa.ease.codegen.model

/**
 * A `@Serializable` type as the schema outputs see it, read from source by `SerialTypeReader` and
 * checked by `SchemaRules`: no KSP types, so outputs and rules are plain functions of it.
 */
internal data class SerialType(
  val serialName: String,
  val kind: TypeKind,
  val doc: String? = null,
  /** The serialized properties of a class; the one property of a value class. */
  val fields: List<SerialField> = emptyList(),
  /** The entries of an enum, by serial name. */
  val values: List<String> = emptyList(),
  /** The serial names of a sealed hierarchy's subtypes. */
  val subtypes: List<String> = emptyList(),
  /** Whether it is written with its serial name under `type`: it belongs to a polymorphic base. */
  val discriminated: Boolean = false,
)

/** The serial names of the types this one refers to: its fields' types and its subtypes. */
internal fun SerialType.references(): List<String> =
  fields.flatMap { it.type.serialNames() } + subtypes

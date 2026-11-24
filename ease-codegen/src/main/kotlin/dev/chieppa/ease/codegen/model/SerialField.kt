package dev.chieppa.ease.codegen.model

/** One serialized property: its serial [name], [type], [doc] and [range], read from its source. */
internal data class SerialField(
  val name: String,
  val type: TypeRef,
  val doc: String? = null,
  val range: ValueRange? = null,
  /** Whether a reader may leave it out: it has a default value. */
  val optional: Boolean = false,
  /** The Kotlin property's name, which differs from [name] under `@SerialName`. */
  val propertyName: String = name,
)

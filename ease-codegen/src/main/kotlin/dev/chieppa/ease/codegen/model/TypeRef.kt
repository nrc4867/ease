package dev.chieppa.ease.codegen.model

/** What a serialized field's type is, as the JSON Schema and the TypeScript declarations say it. */
internal sealed interface TypeRef {
  /** A boolean, a number or a string. */
  data class Primitive(val kind: PrimitiveKind) : TypeRef

  /** A described type, such as a class, an enum or a value class, by its serial name. */
  data class Named(val serialName: String) : TypeRef

  /** A list, set or array of [element]. */
  data class ListOf(val element: TypeRef) : TypeRef

  /** A map from [key] to [value]; JSON keys are strings. */
  data class MapOf(val key: TypeRef, val value: TypeRef) : TypeRef

  /** [inner], or null. */
  data class Nullable(val inner: TypeRef) : TypeRef

  /**
   * A value of an open hierarchy, such as a command or a component, whose subtypes are registered
   * at runtime: its serial name under `type` and that type's fields. [base] is the hierarchy's
   * root.
   */
  data class Open(val base: String) : TypeRef

  /** A type no schema can describe. */
  data object Unknown : TypeRef
}

/** The serial names of every described type [this] refers to, nested references included. */
internal fun TypeRef.serialNames(): List<String> =
  when (this) {
    is TypeRef.Named -> listOf(serialName)
    is TypeRef.ListOf -> element.serialNames()
    is TypeRef.MapOf -> key.serialNames() + value.serialNames()
    is TypeRef.Nullable -> inner.serialNames()
    is TypeRef.Primitive,
    is TypeRef.Open,
    TypeRef.Unknown -> emptyList()
  }

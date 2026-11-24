package dev.chieppa.ease.codegen.model

/** The shape of a [SerialType]. */
internal enum class TypeKind {
  /** A class written as an object of named fields. */
  CLASS,

  /** An enum written as the name of one entry. */
  ENUM,

  /** A value class written as its one property. */
  VALUE,

  /** A sealed hierarchy: one of its subtypes, named by its serial name under `type`. */
  SEALED,
}

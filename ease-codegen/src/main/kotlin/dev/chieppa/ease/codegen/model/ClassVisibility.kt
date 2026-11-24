package dev.chieppa.ease.codegen.model

/** How far a class can be seen, taking every class it is nested in into account. */
internal enum class ClassVisibility {
  /** Public, and so is every class around it: its serializer is public. */
  PUBLIC,

  /** Internal, or nested in an internal class: its serializer is internal. */
  INTERNAL,

  /** Private or protected, or nested in such a class: no serializer beside it can name it. */
  HIDDEN,
}

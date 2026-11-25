package dev.chieppa.ease.codegen.model

/** How a field goes on the wire. */
internal enum class FieldKind {
  FLOAT,
  QUANTIZED_FLOAT,
  INT,
  BOOLEAN,

  /** An enum, sent as its ordinal. */
  ENUM,
}

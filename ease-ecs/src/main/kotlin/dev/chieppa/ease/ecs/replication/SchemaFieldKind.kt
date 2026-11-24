package dev.chieppa.ease.ecs.replication

/** What a [SchemaField] holds and how it goes on the wire. */
public enum class SchemaFieldKind {
  /** A float at full precision. */
  FLOAT,

  /** A float quantized over a range. */
  QUANTIZED_FLOAT,

  /** A 32-bit int. */
  INT,

  /** A boolean, in one bit. */
  BOOLEAN,
}

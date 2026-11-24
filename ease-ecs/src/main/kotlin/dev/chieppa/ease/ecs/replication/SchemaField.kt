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

/**
 * One field of a [ReplicatedSchema], with the options a `@Replicated` class's property takes:
 * [quantization] (for [SchemaFieldKind.QUANTIZED_FLOAT] only), [interpolated] and [audience].
 */
public class SchemaField
internal constructor(
  public val name: String,
  public val kind: SchemaFieldKind,
  public val interpolated: Boolean,
  public val audience: Audience,
  public val quantization: Quantization? = null,
)

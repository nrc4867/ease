package dev.chieppa.ease.ecs.replication

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

package dev.chieppa.ease.ecs.replication

/**
 * Sends a float in [bits] bits spread evenly over the field's `@Range`; values outside it are
 * clamped. The field must carry a `@Range`. A float without `@Quantized` is sent at full precision.
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.BINARY)
public annotation class Quantized(val bits: Int)

package dev.chieppa.ease.ecs.replication

/**
 * Sends a float in [bits] bits spread evenly over [min] to [max]; values outside are clamped. A
 * float without it is sent at full precision.
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.BINARY)
public annotation class Quantized(val bits: Int, val min: Float, val max: Float)

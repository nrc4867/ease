package dev.chieppa.ease.ecs.replication

/**
 * Marks a component whose state replicates. KSP writes its [ReplicatedSerializer] at build time as
 * an object named after the class with `Replicated` appended. Every public `var` with a backing
 * field replicates, in declaration order; the class needs a constructor that takes no arguments.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
public annotation class Replicated

/**
 * Sends a float in [bits] bits spread evenly over [min] to [max]; values outside are clamped. A
 * float without it is sent at full precision.
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.BINARY)
public annotation class Quantized(val bits: Int, val min: Float, val max: Float)

/** Replicas blend this field between received snapshots instead of jumping to each one. */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.BINARY)
public annotation class Interpolated

/** Who receives this field; a field without it goes to [Audience.EVERYONE]. */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.BINARY)
public annotation class ReplicatedTo(val audience: Audience)

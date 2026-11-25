package dev.chieppa.ease.ecs.replication

/**
 * Replicas blend this Float field between received snapshots instead of jumping to each one,
 * linearly; or, when four fields name the same [rotation] group, as the components of one unit
 * quaternion, through the shorter way round and renormalized, so a blend stays a rotation.
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.BINARY)
public annotation class Interpolated(val rotation: String = "")

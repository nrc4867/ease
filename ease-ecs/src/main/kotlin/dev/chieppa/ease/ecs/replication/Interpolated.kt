package dev.chieppa.ease.ecs.replication

/** Replicas blend this field between received snapshots instead of jumping to each one. */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.BINARY)
public annotation class Interpolated

package dev.chieppa.ease.ecs.replication

/** Marks a component whose state replicates; KSP writes its serializer at build time. */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
public annotation class Replicated

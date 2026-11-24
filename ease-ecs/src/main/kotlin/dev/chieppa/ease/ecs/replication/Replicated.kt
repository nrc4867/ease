package dev.chieppa.ease.ecs.replication

/**
 * Marks a component whose state replicates. KSP writes its [ReplicatedSerializer] at build time as
 * an object named after the class with `Replicated` appended. Every public `var` with a backing
 * field replicates, in declaration order; the class needs a constructor that takes no arguments.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
public annotation class Replicated

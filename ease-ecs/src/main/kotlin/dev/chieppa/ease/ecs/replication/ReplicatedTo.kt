package dev.chieppa.ease.ecs.replication

/** Who receives this field; a field without it goes to [Audience.EVERYONE]. */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.BINARY)
public annotation class ReplicatedTo(val audience: Audience)

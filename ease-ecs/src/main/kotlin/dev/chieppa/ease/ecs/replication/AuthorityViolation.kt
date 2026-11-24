package dev.chieppa.ease.ecs.replication

/**
 * A replicated component was written by a world that does not hold authority over its entity, or
 * changed outside [Replication.write]. Thrown in debug builds, naming the entity and component.
 */
public class AuthorityViolation(message: String) : IllegalStateException(message)

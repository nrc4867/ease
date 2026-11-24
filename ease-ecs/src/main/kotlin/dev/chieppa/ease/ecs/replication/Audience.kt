package dev.chieppa.ease.ecs.replication

/** Which peers receive a replicated field, relative to the entity's owner. */
public enum class Audience {
  /** Every peer. */
  EVERYONE,

  /** Only the entity's owner, such as its own ammunition count. */
  OWNER_ONLY,

  /** Every peer except the owner, such as a position the owner predicts itself. */
  ALL_BUT_OWNER,
}

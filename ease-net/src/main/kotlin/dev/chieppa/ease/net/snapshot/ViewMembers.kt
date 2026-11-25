package dev.chieppa.ease.net.snapshot

/** The entities in one peer's view, as its snapshots read them. */
internal interface ViewMembers {
  /** How many spawn and despawn messages the peer has been sent. */
  val sequence: Long

  /** How many entities are in view. */
  val count: Int

  /** The entity id of the [index]th entity in view, in no particular order. */
  fun memberAt(index: Int): Int

  /** The network id of the entity in view at entity id [entityId]. */
  fun networkIdOf(entityId: Int): Long

  /** Whether the peer owns the entity in view at entity id [entityId], as its spawn said. */
  fun isOwner(entityId: Int): Boolean
}

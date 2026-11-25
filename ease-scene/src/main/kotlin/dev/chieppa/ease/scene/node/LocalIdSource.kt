package dev.chieppa.ease.scene.node

import kotlin.random.Random

/** Where new local ids come from, so tests can hand out known ones. */
public fun interface LocalIdSource {
  /** An id for a new node. */
  public fun next(): LocalId

  public companion object {
    /** Random ids, which two people adding nodes on two branches never share in practice. */
    public val random: LocalIdSource = LocalIdSource { LocalId(Random.nextLong()) }
  }
}

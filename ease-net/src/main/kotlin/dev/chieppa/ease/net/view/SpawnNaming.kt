package dev.chieppa.ease.net.view

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World

/** How the host names each entity it spawns on a peer; asked once per spawn, so it may allocate. */
public fun interface SpawnNaming {
  /** The form [entity] of [world] is spawned in. */
  public fun formOf(world: World, entity: Entity): SpawnForm

  public companion object {
    /** Names every entity [SpawnForm.Plain]. */
    public val PLAIN: SpawnNaming = SpawnNaming { _, _ -> SpawnForm.Plain }
  }
}

package dev.chieppa.ease.ecs

import com.github.quillraven.fleks.World
import com.github.quillraven.fleks.configureWorld
import dev.chieppa.ease.ecs.replication.REPLICATION_INJECTABLE
import dev.chieppa.ease.ecs.replication.Replication
import dev.chieppa.ease.ecs.replication.ReplicationGuardSystem
import dev.chieppa.ease.ecs.replication.ReplicationSettings
import dev.chieppa.ease.ecs.replication.identify
import dev.chieppa.ease.ecs.replication.replication

/** The entity capacity a world starts with. */
public const val DEFAULT_ENTITY_CAPACITY: Int = 512

/**
 * Builds a world whose systems run in [Phase] order on every `update`, whatever order [systems]
 * registers them in. Systems are constructed inside [systems], where the world being built is
 * current. Every entity gets a network id and authority held by the world's peer: `spawnEntity`
 * adds them inside the entity's creation, and an entity made with Fleks's own `entity` gets them
 * right after. With [ReplicationSettings.debugChecks], each update ends by checking that replicated
 * components changed only through [Replication.write].
 */
public fun phasedWorld(
  entityCapacity: Int = DEFAULT_ENTITY_CAPACITY,
  replication: ReplicationSettings = ReplicationSettings(),
  systems: PhasedSystems.() -> Unit,
): World {
  val state = Replication(replication)
  val world =
    configureWorld(entityCapacity) {
      injectables { add(REPLICATION_INJECTABLE, state) }
      onAddEntity { entity -> this.replication.identify(this, entity) }
      systems {
        PhasedSystems().apply(systems).inRunOrder().forEach { add(it) }
        if (state.debugChecks) add(ReplicationGuardSystem(state))
      }
    }
  state.attach(world)
  return world
}

package dev.chieppa.ease.ecs

import com.github.quillraven.fleks.World
import com.github.quillraven.fleks.configureWorld

/** The entity capacity a world starts with. */
public const val DEFAULT_ENTITY_CAPACITY: Int = 512

/**
 * Builds a world whose systems run in [Phase] order on every `update`, whatever order [systems]
 * registers them in. Systems are constructed inside [systems], where the world being built is
 * current.
 */
public fun phasedWorld(
  entityCapacity: Int = DEFAULT_ENTITY_CAPACITY,
  systems: PhasedSystems.() -> Unit,
): World =
  configureWorld(entityCapacity) {
    systems { PhasedSystems().apply(systems).inRunOrder().forEach { add(it) } }
  }

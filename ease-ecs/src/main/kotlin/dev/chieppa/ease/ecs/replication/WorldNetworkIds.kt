package dev.chieppa.ease.ecs.replication

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World

/** The live entity carrying [id] in [this] world, or [Entity.NONE] when none does. */
public fun World.entityOf(id: NetworkId): Entity = replication.entities.entityOf(id)

/** [entity]'s network id in [this] world. */
public fun World.networkIdOf(entity: Entity): NetworkId = replication.networkIdOf(entity)

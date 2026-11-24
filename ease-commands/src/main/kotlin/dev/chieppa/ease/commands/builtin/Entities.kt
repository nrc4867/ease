package dev.chieppa.ease.commands.builtin

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.Snapshot
import com.github.quillraven.fleks.World
import dev.chieppa.ease.commands.CommandRejected
import dev.chieppa.ease.ecs.replication.NetworkId
import dev.chieppa.ease.ecs.replication.NetworkIdentity
import dev.chieppa.ease.ecs.replication.entityOf
import dev.chieppa.ease.ecs.replication.replication

/** The live entity [id] names in [world]; rejects the command when none carries it. */
internal fun liveEntity(world: World, id: NetworkId): Entity {
  val entity = world.entityOf(id)
  if (entity == Entity.NONE) throw CommandRejected("Entity $id does not exist")
  return entity
}

/** The live entity [id] names, which [world] must hold authority over; rejects otherwise. */
internal fun heldEntity(world: World, id: NetworkId): Entity {
  val entity = liveEntity(world, id)
  if (!world.replication.hasAuthority(entity)) {
    throw CommandRejected("Entity $id is held by another peer")
  }
  return entity
}

/** [snapshot] without the entity's network identity, which commands carry beside it. */
internal fun withoutIdentity(snapshot: Snapshot): Snapshot =
  Snapshot(snapshot.components.filterNot { it is NetworkIdentity }, snapshot.tags)

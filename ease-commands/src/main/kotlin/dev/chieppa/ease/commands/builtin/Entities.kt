package dev.chieppa.ease.commands.builtin

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.Snapshot
import com.github.quillraven.fleks.World
import dev.chieppa.ease.commands.CommandRejected
import dev.chieppa.ease.ecs.replication.Authority
import dev.chieppa.ease.ecs.replication.NetworkId
import dev.chieppa.ease.ecs.replication.NetworkIdentity
import dev.chieppa.ease.ecs.replication.entityOf
import dev.chieppa.ease.ecs.replication.replication

/**
 * Why the entity [id] names cannot be changed in [world], or null when it can: it must be live and
 * held by [world]'s peer.
 */
internal fun heldRefusal(world: World, id: NetworkId): String? {
  val entity = world.entityOf(id)
  return when {
    entity == Entity.NONE -> "Entity $id does not exist"
    !world.replication.hasAuthority(entity) -> "Entity $id is held by another peer"
    else -> null
  }
}

/** Throws [CommandRejected] with [refusal] when there is one. */
internal fun rejectIf(refusal: String?) {
  if (refusal != null) throw CommandRejected(refusal)
}

/**
 * Why an entity made from [snapshot] cannot be created in [world], or null when it can: it may not
 * carry its own network identity, and an `Authority` in it must be held by [world]'s peer, since
 * moving authority is its own message, never a side effect of creating an entity.
 */
internal fun snapshotRefusal(world: World, snapshot: Snapshot): String? {
  val authority = snapshot.components.filterIsInstance<Authority>().firstOrNull()
  val localPeer = world.replication.localPeer
  return when {
    snapshot.components.any { it is NetworkIdentity } ->
      "A created entity takes its network id from the world or the command, not its components"
    authority != null && authority.holder != localPeer ->
      "A created entity is held by this world's $localPeer, not ${authority.holder}"
    else -> null
  }
}

/** [snapshot] without the entity's network identity, which commands carry beside it. */
internal fun withoutIdentity(snapshot: Snapshot): Snapshot =
  Snapshot(snapshot.components.filterNot { it is NetworkIdentity }, snapshot.tags)

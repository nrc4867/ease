package dev.chieppa.ease.commands

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import dev.chieppa.ease.ecs.replication.Authority
import dev.chieppa.ease.ecs.replication.NetworkId
import dev.chieppa.ease.ecs.replication.PeerId
import dev.chieppa.ease.ecs.replication.entityOf
import dev.chieppa.ease.ecs.replication.replication

/**
 * Why [sender] may not send [command] to [world], or null when it may: the permission layer, worked
 * out from the command's [Command.scope] and who owns the entities it [targets][Command.targets].
 * The world's own peer may send any command.
 */
public fun permissionRefusal(world: World, command: Command, sender: PeerId): String? {
  if (sender == world.replication.localPeer) return null
  return when (command.scope) {
    CommandScope.LOCAL -> "Only ${world.replication.localPeer} may send this command"
    CommandScope.ANY_PEER -> null
    CommandScope.OWNED_ENTITIES -> ownershipRefusal(world, command, sender)
  }
}

private fun ownershipRefusal(world: World, command: Command, sender: PeerId): String? {
  val targets = command.targets()
  var refusal = if (targets.isEmpty()) "The command names no entity $sender owns" else null
  var index = 0
  while (refusal == null && index < targets.size) {
    refusal = targetRefusal(world, targets[index++], sender)
  }
  return refusal
}

private fun targetRefusal(world: World, id: NetworkId, sender: PeerId): String? {
  val entity = world.entityOf(id)
  val authority = if (entity == Entity.NONE) null else with(world) { entity.getOrNull(Authority) }
  return when {
    entity == Entity.NONE -> "Entity $id does not exist"
    authority == null -> "Entity $id belongs to no peer"
    authority.owner != sender -> "Entity $id belongs to ${authority.owner}, not $sender"
    else -> null
  }
}

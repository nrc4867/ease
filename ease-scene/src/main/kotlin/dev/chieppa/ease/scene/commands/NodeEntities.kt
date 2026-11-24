package dev.chieppa.ease.scene.commands

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.Snapshot
import com.github.quillraven.fleks.World
import dev.chieppa.ease.commands.CommandRejected
import dev.chieppa.ease.ecs.replication.Authority
import dev.chieppa.ease.ecs.replication.NetworkIdentity
import dev.chieppa.ease.ecs.replication.replication
import dev.chieppa.ease.scene.node.NodeKey
import dev.chieppa.ease.scene.runtime.SceneNodes

/** [world]'s scene state, or null when the world has no [SceneNodes] system. */
internal fun sceneNodesOf(world: World): SceneNodes? = world.systemOrNull<SceneNodes>()

/** Why [world] cannot take scene commands, or null when it has a [SceneNodes] system. */
internal fun sceneRefusal(world: World): String? =
  if (sceneNodesOf(world) == null) "The world has no scene nodes system" else null

/**
 * Why the node [key] cannot be changed in [world], or null when it can: the world takes scene
 * commands, the node is live, and [world]'s peer holds it.
 */
internal fun heldNodeRefusal(world: World, key: NodeKey): String? {
  val nodes = sceneNodesOf(world) ?: return sceneRefusal(world)
  val entity = nodes.index[key]
  return when {
    entity == Entity.NONE -> "Node $key does not exist"
    !world.replication.hasAuthority(entity) -> "Node $key is held by another peer"
    else -> null
  }
}

/**
 * Why a node made from [snapshot] cannot be created in [world], or null when it can: it carries
 * exactly one key, no live node has it, it carries no network identity of its own, and an
 * `Authority` in it is held by [world]'s peer.
 */
internal fun spawnRefusal(world: World, snapshot: Snapshot): String? {
  val nodes = sceneNodesOf(world) ?: return sceneRefusal(world)
  val keys = snapshot.components.filterIsInstance<NodeKey>()
  val authority = snapshot.components.filterIsInstance<Authority>().firstOrNull()
  val local = world.replication.localPeer
  return when {
    keys.size != 1 -> "A node carries exactly one NodeKey, not ${keys.size}"
    keys[0] in nodes.index -> "Node ${keys[0]} already exists"
    snapshot.components.any { it is NetworkIdentity } ->
      "A node takes its network id from the world, not its components"
    authority != null && authority.holder != local ->
      "A created node is held by this world's $local, not ${authority.holder}"
    else -> null
  }
}

/** Throws [CommandRejected] with [refusal] when there is one. */
internal fun rejectIf(refusal: String?) {
  if (refusal != null) throw CommandRejected(refusal)
}

/** [world]'s scene state; call only after a refusal check found none. */
internal fun sceneNodes(world: World): SceneNodes = checkNotNull(sceneNodesOf(world))

/** [snapshot] without the entity's network identity, which a new entity is given afresh. */
internal fun withoutIdentity(snapshot: Snapshot): Snapshot =
  Snapshot(snapshot.components.filterNot { it is NetworkIdentity }, snapshot.tags)

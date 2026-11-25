package dev.chieppa.ease.scene.commands

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.Snapshot
import com.github.quillraven.fleks.World
import dev.chieppa.ease.commands.builtin.snapshotRefusal
import dev.chieppa.ease.ecs.replication.replication
import dev.chieppa.ease.scene.node.NodeKey
import dev.chieppa.ease.scene.runtime.SceneNodes

/** [world]'s scene state, or null when the world has no [SceneNodes] system. */
internal fun sceneNodesOf(world: World): SceneNodes? = world.systemOrNull<SceneNodes>()

/** [world]'s scene state; call only after a refusal check found the system. */
internal fun sceneNodes(world: World): SceneNodes = checkNotNull(sceneNodesOf(world))

private const val NO_SCENES = "The world has no scene nodes system"

/**
 * Why the node [key] cannot be changed in [world], or null when it can: the world takes scene
 * commands, the node is live, and [world]'s peer holds it.
 */
internal fun heldNodeRefusal(world: World, key: NodeKey): String? {
  val nodes = sceneNodesOf(world) ?: return NO_SCENES
  val entity = nodes.index[key]
  return when {
    entity == Entity.NONE -> "Node $key does not exist"
    !world.replication.hasAuthority(entity) -> "Node $key is held by another peer"
    else -> null
  }
}

/**
 * Why a node made from [snapshot] cannot be created in [world], or null when it can: the world
 * takes scene commands, the snapshot carries exactly one key, and it passes the check every created
 * entity passes, which refuses a key already live.
 */
internal fun spawnRefusal(world: World, snapshot: Snapshot): String? {
  val keys = snapshot.components.count { it is NodeKey }
  return when {
    sceneNodesOf(world) == null -> NO_SCENES
    keys != 1 -> "A node carries exactly one NodeKey, not $keys"
    else -> snapshotRefusal(world, snapshot)
  }
}

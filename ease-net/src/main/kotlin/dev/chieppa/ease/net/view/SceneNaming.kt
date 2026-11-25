package dev.chieppa.ease.net.view

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import dev.chieppa.ease.scene.node.NodeKey
import dev.chieppa.ease.scene.runtime.SceneSync

/**
 * Names a node that [scenes] spawned by the asset it placed and the node's key, and every other
 * entity, a node an editor command made among them, [SpawnForm.Plain].
 */
public class SceneNaming(private val scenes: SceneSync) : SpawnNaming {
  override fun formOf(world: World, entity: Entity): SpawnForm {
    val key = with(world) { entity.getOrNull(NodeKey) }
    val placed = key?.let { scenes.placementOf(it) }
    return if (key == null || placed == null || key.depth > ViewMessages.MAX_KEY_DEPTH) {
      SpawnForm.Plain
    } else {
      SpawnForm.SceneNode(placed.asset, placed.placement?.depth ?: 0, key)
    }
  }
}

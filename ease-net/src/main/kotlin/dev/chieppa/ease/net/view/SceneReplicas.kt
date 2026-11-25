package dev.chieppa.ease.net.view

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.World
import dev.chieppa.ease.assets.Guid
import dev.chieppa.ease.assets.loading.AssetStore
import dev.chieppa.ease.scene.node.NodeKey
import dev.chieppa.ease.scene.runtime.ResolvedScenes
import dev.chieppa.ease.scene.runtime.SceneNodes

/**
 * Builds the scene nodes a host spawns from this client's own files in [store]: the placed asset is
 * resolved as the host resolved it, overrides applied, and the named node's scene components are
 * decoded with the types [world]'s `SceneNodes` system knows.
 */
public class SceneReplicas(world: World, store: AssetStore) : ReplicaContent {
  private val nodes = world.systemOrNull<SceneNodes>()
  private val scenes = nodes?.let { ResolvedScenes(it.types, store) }

  override fun nodeRefusal(asset: Guid, placement: NodeKey?, key: NodeKey): ViewRefusal? {
    val scenes = scenes ?: return ViewRefusal.NO_SCENES
    return when {
      !scenes.hasAsset(asset) -> ViewRefusal.UNKNOWN_ASSET
      scenes.resolve(asset, placement)[key] == null -> ViewRefusal.UNKNOWN_NODE
      key in checkNotNull(nodes).index -> ViewRefusal.NODE_EXISTS
      else -> null
    }
  }

  override fun node(asset: Guid, placement: NodeKey?, key: NodeKey): List<Component<out Any>> {
    val node = checkNotNull(checkNotNull(scenes).resolve(asset, placement)[key])
    val types = checkNotNull(nodes).types
    val components = mutableListOf<Component<out Any>>(key)
    node.components.forEach { (name, fields) ->
      types[name]?.let { components += it.decode(fields) }
    }
    return components
  }
}

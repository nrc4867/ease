package dev.chieppa.ease.scene.runtime

import dev.chieppa.ease.assets.Guid
import dev.chieppa.ease.assets.loading.AssetStore
import dev.chieppa.ease.scene.node.NodeKey
import dev.chieppa.ease.scene.resolve.ResolvedScene
import dev.chieppa.ease.scene.types.SceneTypes

/**
 * Scenes resolved from this peer's own files and kept until a file they were built from reloads,
 * appears or changes: what a client builds the replicas of a host's scene nodes from, so that a
 * burst of spawns from one scene resolves it once.
 */
public class ResolvedScenes(private val types: SceneTypes, private val store: AssetStore) {
  private class Entry(val placed: PlacedScene, val scene: ResolvedScene)

  private val entries = HashMap<Pair<Guid, NodeKey?>, Entry>()

  /** The scene or prefab [asset] placed under [placement] (null for the outermost scene). */
  public fun resolve(asset: Guid, placement: NodeKey?): ResolvedScene {
    val key = asset to placement
    val cached = entries[key]
    if (cached != null && !cached.placed.isStale(store.database)) return cached.scene
    val source = TrackingSource(store)
    val scene = source.resolver(types).resolve(asset, placement)
    val placed = PlacedScene(asset, placement)
    source.trackIn(placed)
    entries[key] = Entry(placed, scene)
    return scene
  }

  /** Whether this peer's asset database has an asset [asset]. */
  public fun hasAsset(asset: Guid): Boolean = store.database[asset] != null
}

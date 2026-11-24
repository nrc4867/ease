package dev.chieppa.ease.scene.runtime

import dev.chieppa.ease.assets.Guid
import dev.chieppa.ease.assets.loading.AssetHandle
import dev.chieppa.ease.scene.document.SceneDocument
import dev.chieppa.ease.scene.node.NodeKey
import dev.chieppa.ease.scene.resolve.SceneProblem

/**
 * A scene or prefab [asset] spawned into a world under [placement] (null for the outermost scene),
 * as [SceneSync] keeps it: the keys it spawned, the handles of every file it was built from with
 * the version each had, and what could not be honored the last time it was applied.
 */
public class PlacedScene
internal constructor(
  public val asset: Guid,
  public val placement: NodeKey?,
) {
  /** The keys of the nodes the last applied version spawned. */
  public var keys: Set<NodeKey> = emptySet()
    internal set

  /** What the last applied version could not honor. */
  public var problems: List<SceneProblem> = emptyList()
    internal set

  private var handles: Array<AssetHandle<SceneDocument>> = emptyArray()
  private var versions: IntArray = IntArray(0)

  internal fun track(used: List<AssetHandle<SceneDocument>>) {
    handles = used.toTypedArray()
    versions = IntArray(handles.size) { handles[it].version }
  }

  /** Whether a file this placement was built from has loaded again since; never allocates. */
  internal fun isStale(): Boolean {
    for (index in handles.indices) if (handles[index].version != versions[index]) return true
    return false
  }
}

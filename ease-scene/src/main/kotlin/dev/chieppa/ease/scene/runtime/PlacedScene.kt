package dev.chieppa.ease.scene.runtime

import dev.chieppa.ease.assets.Guid
import dev.chieppa.ease.assets.database.AssetDatabase
import dev.chieppa.ease.assets.loading.AssetHandle
import dev.chieppa.ease.scene.document.SceneDocument
import dev.chieppa.ease.scene.node.NodeKey
import dev.chieppa.ease.scene.resolve.SceneProblem

/**
 * A scene or prefab [asset] spawned into a world under [placement] (null for the outermost scene),
 * as [SceneSync] keeps it: the keys it spawned, parents first; the handles of every file it was
 * built from with the version each had; the files it needed but could not load, with the content
 * hash each had then (null when it did not exist); and what could not be honored the last time it
 * was applied.
 */
public class PlacedScene
internal constructor(
  public val asset: Guid,
  public val placement: NodeKey?,
) {
  /** The keys of the nodes the last applied version spawned, parents first. */
  public var keys: List<NodeKey> = emptyList()
    internal set

  /** What the last applied version could not honor. */
  public var problems: List<SceneProblem> = emptyList()
    internal set

  private var handles: Array<AssetHandle<SceneDocument>> = emptyArray()
  private var versions: IntArray = IntArray(0)
  private var missing: Array<Guid> = emptyArray()
  private var missingHashes: Array<String?> = emptyArray()

  internal fun track(used: List<AssetHandle<SceneDocument>>, failed: Map<Guid, String?>) {
    handles = used.toTypedArray()
    versions = IntArray(handles.size) { handles[it].version }
    missing = failed.keys.toTypedArray()
    missingHashes = Array(missing.size) { failed[missing[it]] }
  }

  /**
   * Whether a file this placement was built from has loaded again since, or a file it could not
   * load has appeared or changed in [database]; never allocates.
   */
  internal fun isStale(database: AssetDatabase): Boolean {
    var stale = false
    for (index in handles.indices) stale = stale || handles[index].version != versions[index]
    for (index in missing.indices) {
      stale = stale || database[missing[index]]?.contentHash != missingHashes[index]
    }
    return stale
  }
}

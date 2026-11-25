package dev.chieppa.ease.net.view

import dev.chieppa.ease.assets.Guid
import dev.chieppa.ease.scene.node.NodeKey

/** What a spawn names a replica by, so the receiving peer builds it from its own files. */
public sealed interface SpawnForm {
  /**
   * No asset: the replica is the network id, the authority and the replicated components the spawn
   * carries; components that do not replicate stay on the host.
   */
  public data object Plain : SpawnForm

  /**
   * A scene node: the scene or prefab [asset] that was placed, how many leading ids of [key] are
   * the placement it was placed under (0 for an outermost scene), and the node's [key]. The peer
   * resolves the same asset under the same placement from its own files and builds the node it
   * finds under [key], instance overrides applied, with the spawn's replicated state on top.
   */
  public class SceneNode(
    public val asset: Guid,
    public val placementDepth: Int,
    public val key: NodeKey,
  ) : SpawnForm {
    init {
      require(placementDepth in 0 until key.depth) {
        "A placement is 0 to ${key.depth - 1} ids of $key, not $placementDepth"
      }
    }

    /** The placement the node lies in, or null for a node of an outermost scene. */
    public val placement: NodeKey?
      get() = if (placementDepth == 0) null else key.prefix(placementDepth)
  }
}

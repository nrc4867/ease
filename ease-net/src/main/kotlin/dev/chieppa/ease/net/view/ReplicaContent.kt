package dev.chieppa.ease.net.view

import com.github.quillraven.fleks.Component
import dev.chieppa.ease.assets.Guid
import dev.chieppa.ease.scene.node.NodeKey

/**
 * Where a client finds the content a spawn names: the components of a scene node as its own files
 * hold them. Asked once per spawn, so it may allocate.
 */
public interface ReplicaContent {
  /**
   * Why the node [key] of [asset] placed under [placement] cannot be built here, or null when it
   * can.
   */
  public fun nodeRefusal(asset: Guid, placement: NodeKey?, key: NodeKey): ViewRefusal?

  /**
   * The components of the node [key] of [asset] placed under [placement], its key among them; call
   * only after [nodeRefusal] found none.
   */
  public fun node(asset: Guid, placement: NodeKey?, key: NodeKey): List<Component<out Any>>

  public companion object {
    /** Content for a client with no scenes: every scene node is refused. */
    public val NONE: ReplicaContent =
      object : ReplicaContent {
        override fun nodeRefusal(asset: Guid, placement: NodeKey?, key: NodeKey): ViewRefusal =
          ViewRefusal.NO_SCENES

        override fun node(asset: Guid, placement: NodeKey?, key: NodeKey) =
          error("A client without scenes builds no scene node")
      }
  }
}

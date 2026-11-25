package dev.chieppa.ease.scene.resolve

import dev.chieppa.ease.assets.Guid
import dev.chieppa.ease.scene.node.NodeKey

/**
 * Something in a scene that spawning could not honor. The rest of the scene still spawns, so one
 * bad node never stops a game; the problems say what was skipped.
 */
public sealed interface SceneProblem {
  /** A sentence naming the node and what is wrong. */
  public val message: String

  /** A node names a parent its file does not have; it hangs from the placement instead. */
  public class MissingParent(public val node: NodeKey) : SceneProblem {
    override val message: String
      get() = "Node $node names a parent that does not exist"
  }

  /** A component this build has no type for, or whose fields do not fit its type. */
  public class BadComponent(
    public val node: NodeKey,
    public val component: String,
    public val reason: String,
  ) : SceneProblem {
    override val message: String
      get() = "Node $node has component $component that does not load: $reason"
  }

  /** A placement or asset slot names an asset the project does not have. */
  public class MissingAsset(public val node: NodeKey, public val asset: Guid) : SceneProblem {
    override val message: String
      get() = "Node $node names asset $asset, which does not exist"
  }

  /** A placement that would contain itself, directly or through others. */
  public class Cycle(public val node: NodeKey, public val asset: Guid) : SceneProblem {
    override val message: String
      get() = "Node $node places $asset inside itself"
  }

  /** A command bringing the world in line with the scene was rejected, for [reason]. */
  public class Rejected(public val reason: String) : SceneProblem {
    override val message: String
      get() = "A scene change was rejected: $reason"
  }

  /** An override naming a node or component the placed asset lacks; it stays in the file. */
  public class StaleOverride(public val instance: NodeKey, public val path: String) : SceneProblem {
    override val message: String
      get() = "Instance $instance overrides $path, which the placed asset does not have"
  }
}

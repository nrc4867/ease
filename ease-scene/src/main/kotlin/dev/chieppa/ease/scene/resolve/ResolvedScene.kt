package dev.chieppa.ease.scene.resolve

import dev.chieppa.ease.assets.Guid
import dev.chieppa.ease.scene.node.NodeKey

/**
 * A scene flattened for spawning: every node, from every prefab and sublevel placed in it, under
 * its full key, parents before children; the [assets] it was built from, the scene's own first; and
 * what could not be honored.
 */
public class ResolvedScene(
  public val nodes: List<ResolvedNode>,
  public val assets: List<Guid>,
  public val problems: List<SceneProblem>,
) {
  private val byKey = nodes.associateBy { it.key }

  /** The node [key], or null when the scene has none. */
  public operator fun get(key: NodeKey): ResolvedNode? = byKey[key]
}

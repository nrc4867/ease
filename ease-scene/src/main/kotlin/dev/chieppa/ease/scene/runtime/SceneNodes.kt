package dev.chieppa.ease.scene.runtime

import com.github.quillraven.fleks.IntervalSystem
import dev.chieppa.ease.scene.types.SceneTypes

/**
 * A world's scene state: the component [types] its scenes save and spawn, and the [index] of its
 * live nodes by key. Scene commands find both through this system, so a command from the editor, an
 * agent or a peer changes nodes the same way a reload does. It has no per-tick work.
 */
public class SceneNodes(public val types: SceneTypes = SceneTypes.build()) : IntervalSystem() {
  /** The live nodes by key. */
  public val index: NodeIndex = NodeIndex()

  override fun onTick(): Unit = Unit
}

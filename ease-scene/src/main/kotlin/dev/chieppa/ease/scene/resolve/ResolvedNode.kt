package dev.chieppa.ease.scene.resolve

import dev.chieppa.ease.scene.node.NodeKey
import java.util.SortedMap
import kotlinx.serialization.json.JsonObject

/**
 * One node as it spawns: its full [key], and every component it carries by serial name with its
 * slots resolved and overrides applied, structure ([dev.chieppa.ease.scene.node.Parent] and
 * [dev.chieppa.ease.scene.node.NodeName]) included. Fields are in the form their type writes, so
 * comparing them with a live entity's components is comparing JSON. [failed] names the components
 * the file holds that did not load; a live node keeps its last good value of those.
 */
public class ResolvedNode(
  public val key: NodeKey,
  public val components: SortedMap<String, JsonObject>,
  public val failed: Set<String> = emptySet(),
) {
  override fun toString(): String = "ResolvedNode($key)"
}

package dev.chieppa.ease.project.scene

import dev.chieppa.ease.assets.Guid
import dev.chieppa.ease.scene.document.Instance
import dev.chieppa.ease.scene.document.SceneNode
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * The asset references a node holds. Without the game's component types, a reference is any text
 * value that is exactly a GUID, which is how asset slots are written, plus the asset a placement
 * names.
 */
public object GuidScan {
  private val GUID_TEXT =
    Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")

  /** Every GUID [text] contains, in any case, in order of appearance. */
  public fun guidsIn(text: String): List<Guid> =
    GUID_TEXT.findAll(text).map { Guid.parse(it.value) }.toList()

  /** The assets [node] refers to: its placement's, and every GUID in a component or override. */
  public fun referencesIn(node: SceneNode): Set<Guid> {
    val found = linkedSetOf<Guid>()
    node.instance?.let { instance ->
      found += instance.asset
      instance.overrides.values.forEach { collect(it, found) }
    }
    node.components.values.forEach { collect(it, found) }
    return found
  }

  /** [node] with each reference found in [mapping] replaced by what it maps to. */
  public fun rewrite(node: SceneNode, mapping: Map<Guid, Guid>): SceneNode =
    node.copy(
      components =
        node.components.mapValues { (_, fields) -> rewrite(fields, mapping) as JsonObject },
      instance =
        node.instance?.let { instance ->
          Instance(
            mapping[instance.asset] ?: instance.asset,
            instance.overrides.mapValues { (_, value) -> rewrite(value, mapping) },
          )
        },
    )

  private fun collect(element: JsonElement, found: MutableSet<Guid>) {
    when (element) {
      is JsonObject -> element.values.forEach { collect(it, found) }
      is JsonArray -> element.forEach { collect(it, found) }
      is JsonPrimitive ->
        if (element.isString && GUID_TEXT.matches(element.content)) {
          found += Guid.parse(element.content)
        }
    }
  }

  private fun rewrite(element: JsonElement, mapping: Map<Guid, Guid>): JsonElement =
    when (element) {
      is JsonObject -> JsonObject(element.mapValues { rewrite(it.value, mapping) })
      is JsonArray -> JsonArray(element.map { rewrite(it, mapping) })
      is JsonPrimitive ->
        if (element.isString && GUID_TEXT.matches(element.content)) {
          mapping[Guid.parse(element.content)]?.let { JsonPrimitive(it.toString()) } ?: element
        } else {
          element
        }
    }
}

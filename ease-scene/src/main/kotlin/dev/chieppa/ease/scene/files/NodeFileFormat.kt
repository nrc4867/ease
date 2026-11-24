package dev.chieppa.ease.scene.files

import dev.chieppa.ease.assets.Guid
import dev.chieppa.ease.scene.document.Instance
import dev.chieppa.ease.scene.document.OverridePath
import dev.chieppa.ease.scene.document.SceneNode
import dev.chieppa.ease.scene.node.LocalId
import dev.chieppa.ease.scene.node.OrderKey
import dev.chieppa.ease.serialization.format.FORMAT_VERSION_KEY
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private const val NAME = "name"
private const val PARENT = "parent"
private const val ORDER = "order"
private const val COMPONENTS = "components"
private const val INSTANCE = "instance"
private const val ASSET = "asset"
private const val OVERRIDES = "overrides"
private const val SETTINGS = "settings"

/**
 * The tree each scene file holds. A node file has `formatVersion`, `name`, `order`, `components`
 * (serial name to fields), `parent` only when the node has one, and `instance` (`asset` and
 * `overrides`, one key per override path) only for a placement. The node's id is the file's name,
 * so it is not repeated inside. The header file has `formatVersion` and `settings`.
 */
internal object NodeFileFormat {
  fun nodeTree(node: SceneNode): JsonObject {
    val fields =
      linkedMapOf<String, JsonElement>(
        FORMAT_VERSION_KEY to JsonPrimitive(SceneFormat.FORMAT_VERSION),
        NAME to JsonPrimitive(node.name),
        ORDER to JsonPrimitive(node.order.text),
        COMPONENTS to JsonObject(node.components),
      )
    node.parent?.let { fields[PARENT] = JsonPrimitive(it.text) }
    node.instance?.let { fields[INSTANCE] = instanceTree(it) }
    return JsonObject(fields)
  }

  private fun instanceTree(instance: Instance): JsonObject =
    JsonObject(
      mapOf(
        ASSET to JsonPrimitive(instance.asset.toString()),
        OVERRIDES to JsonObject(instance.overrides.mapKeys { it.key.text }),
      )
    )

  /** The node [id] that the migrated [tree] describes. */
  fun node(id: LocalId, tree: JsonObject): SceneNode =
    SceneNode(
      id = id,
      name = text(tree, NAME),
      parent = (tree[PARENT] as? JsonPrimitive)?.let { LocalId.parse(it.content) },
      order = OrderKey(text(tree, ORDER)),
      components =
        (tree[COMPONENTS]?.jsonObject ?: JsonObject(emptyMap())).mapValues {
          it.value.jsonObject
        },
      instance = tree[INSTANCE]?.jsonObject?.let(::instance),
    )

  private fun instance(tree: JsonObject): Instance =
    Instance(
      Guid.parse(text(tree, ASSET)),
      (tree[OVERRIDES]?.jsonObject ?: JsonObject(emptyMap())).mapKeys {
        OverridePath.parse(it.key)
      },
    )

  fun headerTree(settings: JsonObject): JsonObject =
    JsonObject(
      mapOf(
        FORMAT_VERSION_KEY to JsonPrimitive(SceneFormat.FORMAT_VERSION),
        SETTINGS to settings,
      )
    )

  fun settings(tree: JsonObject): JsonObject = tree[SETTINGS]?.jsonObject ?: JsonObject(emptyMap())

  private fun text(tree: JsonObject, key: String): String =
    requireNotNull(tree[key]) { "missing $key" }.jsonPrimitive.content
}

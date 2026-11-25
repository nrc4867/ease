package dev.chieppa.ease.scene.resolve

import dev.chieppa.ease.scene.node.KEY_SEPARATOR
import dev.chieppa.ease.scene.slots.ASSET_REF_NAME
import dev.chieppa.ease.scene.slots.NODE_REF_NAME
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Resolves the slots in a component's fields, guided by its serializer's descriptor: every node
 * slot, written relative to the file it is in, gets the ids of that file's placement in front, and
 * every asset slot's text is handed to [assets] for checking. Fields the descriptor does not
 * describe are left as they are.
 */
internal class SlotRewriter(private val prefix: String, private val assets: (String) -> Unit) {
  /** [element] with its slots resolved, read as [descriptor] says. */
  fun rewrite(descriptor: SerialDescriptor, element: JsonElement): JsonElement {
    val name = descriptor.serialName.removeSuffix("?")
    return when {
      name == NODE_REF_NAME -> nodeRef(element)
      name == ASSET_REF_NAME -> element.also(::assetRef)
      descriptor.isInline -> rewrite(descriptor.getElementDescriptor(0), element)
      descriptor.kind == StructureKind.LIST && element is JsonArray ->
        JsonArray(element.map { rewrite(descriptor.getElementDescriptor(0), it) })
      descriptor.kind == StructureKind.MAP && element is JsonObject ->
        JsonObject(element.mapValues { rewrite(descriptor.getElementDescriptor(1), it.value) })
      descriptor.kind == StructureKind.CLASS && element is JsonObject -> fields(descriptor, element)
      else -> element
    }
  }

  /** [element], an object of [descriptor]'s class, with each described field rewritten. */
  fun fields(descriptor: SerialDescriptor, element: JsonObject): JsonObject =
    JsonObject(
      element.mapValues { (field, value) ->
        val index = descriptor.getElementIndex(field)
        if (index < 0) value else rewrite(descriptor.getElementDescriptor(index), value)
      }
    )

  private fun assetRef(element: JsonElement) {
    val slot = element as? JsonPrimitive
    if (slot != null && slot !is JsonNull) assets(slot.content)
  }

  private fun nodeRef(element: JsonElement): JsonElement {
    val slot = element as? JsonPrimitive
    return if (slot == null || !slot.isString || prefix.isEmpty()) element
    else JsonPrimitive(prefix + KEY_SEPARATOR + slot.content)
  }
}

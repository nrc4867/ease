package dev.chieppa.ease.scene

import dev.chieppa.ease.scene.document.Instance
import dev.chieppa.ease.scene.document.SceneDocument
import dev.chieppa.ease.scene.document.SceneNode
import dev.chieppa.ease.scene.node.LocalId
import dev.chieppa.ease.scene.node.OrderKey
import dev.chieppa.ease.scene.types.SceneTypes
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Helpers that build scenes in code for tests. */
object Scenes {
  /** The scene types tests use: the built-ins and [Beacon]. */
  val types: SceneTypes = SceneTypes.build {
    component(Beacon, Beacon.serializer())
    component(Panel, Panel.serializer())
  }

  const val TRANSFORM = "ease.LocalTransform"
  const val BEACON = "test.Beacon"
  const val PANEL = "test.Panel"

  /** The local id [n], easy to read in failures. */
  fun id(n: Long): LocalId = LocalId(n)

  /** A component's fields from pairs of field name and number or text. */
  fun fields(vararg pairs: Pair<String, Any>): JsonObject =
    JsonObject(
      pairs.associate { (name, value) ->
        name to if (value is Number) JsonPrimitive(value) else JsonPrimitive(value.toString())
      }
    )

  /** A node with a transform at [x], under [parent], after the [order] given. */
  @Suppress("LongParameterList") // Named arguments with defaults keep each test's nodes short.
  fun node(
    id: Long,
    name: String,
    parent: Long? = null,
    order: OrderKey = OrderKey.FIRST,
    x: Float = 0f,
    instance: Instance? = null,
  ): SceneNode =
    SceneNode(
      id(id),
      name,
      parent?.let(::id),
      order,
      mapOf(TRANSFORM to fields("x" to x)),
      instance,
    )

  /** A document holding [nodes]. */
  fun document(vararg nodes: SceneNode): SceneDocument = SceneDocument(nodes = nodes.toList())
}

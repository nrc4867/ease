package dev.chieppa.ease.scene.document

import dev.chieppa.ease.scene.node.LocalId
import dev.chieppa.ease.scene.node.OrderKey
import java.util.SortedMap
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * One node of a scene or prefab file, as its file holds it: its [id] (also its file's name), its
 * [name], its [parent] in the same file (null for a top node), its [order] among its siblings, its
 * [components] by serial name, each as the fields its serializer writes, and, for a placement of
 * another scene or prefab, its [instance]. Components stay as written, so a file with a component
 * this build does not know keeps it when saved.
 */
public class SceneNode(
  public val id: LocalId,
  public val name: String,
  public val parent: LocalId?,
  public val order: OrderKey,
  components: Map<String, JsonObject> = emptyMap(),
  public val instance: Instance? = null,
) {
  /** The components by serial name, in name order. */
  public val components: SortedMap<String, JsonObject> = components.toSortedMap()

  /** This node with the field [field] of the component [component] set to [value]. */
  public fun withField(component: String, field: String, value: JsonElement): SceneNode {
    val fields = components[component] ?: JsonObject(emptyMap())
    return copy(components = components + (component to JsonObject(fields + (field to value))))
  }

  /** A copy with the given parts changed. */
  public fun copy(
    name: String = this.name,
    parent: LocalId? = this.parent,
    order: OrderKey = this.order,
    components: Map<String, JsonObject> = this.components,
    instance: Instance? = this.instance,
  ): SceneNode = SceneNode(id, name, parent, order, components, instance)

  override fun equals(other: Any?): Boolean =
    other is SceneNode &&
      other.id == id &&
      other.name == name &&
      other.parent == parent &&
      other.order == order &&
      other.components == components &&
      other.instance == instance

  override fun hashCode(): Int = id.hashCode()

  override fun toString(): String = "SceneNode($id $name)"
}

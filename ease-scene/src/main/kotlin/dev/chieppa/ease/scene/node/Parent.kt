package dev.chieppa.ease.scene.node

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import dev.chieppa.ease.annotations.Doc
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Doc(
  "Where a node hangs in the tree: its parent's key and its place among the parent's children. " +
    "A node of the outermost scene with no parent carries none."
)
@Serializable
@SerialName("ease.Parent")
public class Parent(
  @Doc("The parent node's key.") public val node: NodeKey,
  @Doc("The node's place among its siblings.") public val order: OrderKey,
) : Component<Parent> {
  override fun type(): ComponentType<Parent> = Parent

  override fun equals(other: Any?): Boolean =
    other is Parent && other.node == node && other.order == order

  override fun hashCode(): Int = node.hashCode() * HASH_PRIME + order.hashCode()

  /** The component type. */
  public companion object : ComponentType<Parent>() {
    private const val HASH_PRIME = 31
  }
}

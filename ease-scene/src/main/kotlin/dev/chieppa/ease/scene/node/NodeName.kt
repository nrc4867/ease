package dev.chieppa.ease.scene.node

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import dev.chieppa.ease.annotations.Doc
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Doc("A node's name, as the outliner shows it. Names need not be unique; keys are.")
@Serializable
@SerialName("ease.NodeName")
public class NodeName(@Doc("The name.") public val name: String) : Component<NodeName> {
  override fun type(): ComponentType<NodeName> = NodeName

  override fun equals(other: Any?): Boolean = other is NodeName && other.name == name

  override fun hashCode(): Int = name.hashCode()

  /** The component type. */
  public companion object : ComponentType<NodeName>()
}

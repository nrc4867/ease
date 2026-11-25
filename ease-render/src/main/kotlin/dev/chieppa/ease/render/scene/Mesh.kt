package dev.chieppa.ease.render.scene

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import dev.chieppa.ease.annotations.Doc
import dev.chieppa.ease.ecs.replication.Replicated
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Doc(
  "Draws a built-in mesh at its entity's LocalTransform, shaded by the entity's Material, or a " +
    "plain white one without it."
)
@Replicated
@Serializable
@SerialName("ease.Mesh")
public class Mesh : Component<Mesh> {
  @Doc("Which built-in mesh to draw.") public var shape: MeshShape = MeshShape.CUBE

  override fun type(): ComponentType<Mesh> = Mesh

  /** The component type, and a factory for a mesh of a shape. */
  public companion object : ComponentType<Mesh>() {
    /** A mesh drawing [shape]. */
    public fun of(shape: MeshShape): Mesh = Mesh().also { it.shape = shape }
  }
}

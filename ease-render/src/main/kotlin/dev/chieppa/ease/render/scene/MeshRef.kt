package dev.chieppa.ease.render.scene

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import dev.chieppa.ease.annotations.Doc
import dev.chieppa.ease.render.assets.MeshAsset
import dev.chieppa.ease.scene.slots.AssetRef
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Doc(
  "Draws a mesh asset, such as one a model imports, at its entity's world transform, in place of " +
    "any built-in Mesh shape. It is saved in scenes and built by every peer from its own files; " +
    "it does not replicate."
)
@Serializable
@SerialName("ease.MeshRef")
public class MeshRef(
  @Doc("The cooked mesh asset to draw; empty draws nothing.")
  public val mesh: AssetRef<MeshAsset>? = null
) : Component<MeshRef> {
  override fun type(): ComponentType<MeshRef> = MeshRef

  override fun equals(other: Any?): Boolean = other is MeshRef && other.mesh == mesh

  override fun hashCode(): Int = mesh.hashCode()

  /** The component type. */
  public companion object : ComponentType<MeshRef>()
}

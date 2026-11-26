package dev.chieppa.ease.render.scene

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import dev.chieppa.ease.annotations.Doc
import dev.chieppa.ease.render.assets.MaterialAsset
import dev.chieppa.ease.scene.slots.AssetRef
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Doc(
  "The material asset its entity's mesh is drawn with, in place of any inline Material. It is " +
    "saved in scenes and built by every peer from its own files; it does not replicate."
)
@Serializable
@SerialName("ease.MaterialRef")
public class MaterialRef(
  @Doc("The material asset; empty falls back to the inline Material, then plain white.")
  public val material: AssetRef<MaterialAsset>? = null
) : Component<MaterialRef> {
  override fun type(): ComponentType<MaterialRef> = MaterialRef

  override fun equals(other: Any?): Boolean = other is MaterialRef && other.material == material

  override fun hashCode(): Int = material.hashCode()

  /** The component type. */
  public companion object : ComponentType<MaterialRef>()
}

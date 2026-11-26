package dev.chieppa.ease.animation.scene

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import dev.chieppa.ease.animation.assets.SkinAsset
import dev.chieppa.ease.annotations.Doc
import dev.chieppa.ease.scene.slots.AssetRef
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Doc(
  "Skins a node's mesh to a skeleton: the skin asset names its joints by their ids in the same " +
    "prefab as this node, and the mesh is drawn bent by where those joints are. It is saved in " +
    "scenes and built by every peer from its own files; it does not replicate."
)
@Serializable
@SerialName("ease.SkinRef")
public class SkinRef(
  @Doc("The cooked skin asset; empty draws the mesh unskinned.")
  public val skin: AssetRef<SkinAsset>? = null
) : Component<SkinRef> {
  override fun type(): ComponentType<SkinRef> = SkinRef

  override fun equals(other: Any?): Boolean = other is SkinRef && other.skin == skin

  override fun hashCode(): Int = skin.hashCode()

  /** The component type. */
  public companion object : ComponentType<SkinRef>()
}

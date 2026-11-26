package dev.chieppa.ease.animation.scene

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import dev.chieppa.ease.animation.assets.AnimatorAsset
import dev.chieppa.ease.annotations.Doc
import dev.chieppa.ease.scene.slots.AssetRef
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Doc(
  "Plays an animator asset on the model this node places: its clips pose the model's bones, " +
    "found under this node by the prefab ids clip channels name. Every peer works out the pose " +
    "from its own files and the parameters it has; neither the pose nor this component replicates."
)
@Serializable
@SerialName("ease.Animator")
public class Animator(
  @Doc("The animator asset to play; empty leaves the model in its rest pose.")
  public val animator: AssetRef<AnimatorAsset>? = null
) : Component<Animator> {
  override fun type(): ComponentType<Animator> = Animator

  override fun equals(other: Any?): Boolean = other is Animator && other.animator == animator

  override fun hashCode(): Int = animator.hashCode()

  /** The component type. */
  public companion object : ComponentType<Animator>()
}

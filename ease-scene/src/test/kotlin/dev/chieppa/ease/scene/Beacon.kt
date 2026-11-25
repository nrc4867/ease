package dev.chieppa.ease.scene

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import dev.chieppa.ease.annotations.Doc
import dev.chieppa.ease.scene.slots.AssetRef
import dev.chieppa.ease.scene.slots.NodeRef
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Doc("A test component with a strength, a node slot and an asset slot.")
@Serializable
@SerialName("test.Beacon")
class Beacon(
  @Doc("How bright it glows.") val strength: Float = 1f,
  @Doc("The node it points at.") val target: NodeRef? = null,
  @Doc("The glow it uses.") val glow: AssetRef<Any>? = null,
) : Component<Beacon> {
  override fun type(): ComponentType<Beacon> = Beacon

  companion object : ComponentType<Beacon>()
}

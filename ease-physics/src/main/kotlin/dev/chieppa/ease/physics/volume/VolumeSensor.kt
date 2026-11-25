package dev.chieppa.ease.physics.volume

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import dev.chieppa.ease.annotations.Doc
import dev.chieppa.ease.annotations.Range
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Doc(
  "Tracks which trigger volumes a node is inside, from one point above its position, each fixed " +
    "step. Its collision layer is its Collider's or CharacterController's, else the first layer."
)
@Serializable
@SerialName("ease.VolumeSensor")
public class VolumeSensor : Component<VolumeSensor> {
  @Doc("How far above the node's position the sensed point is, in world units.")
  @Range(-MAX_OFFSET, MAX_OFFSET)
  public var offsetY: Float = 0f

  override fun type(): ComponentType<VolumeSensor> = VolumeSensor

  /** The component type, and the offset's bounds. */
  public companion object : ComponentType<VolumeSensor>() {
    /** The largest offset, in world units. */
    public const val MAX_OFFSET: Double = 100.0
  }
}

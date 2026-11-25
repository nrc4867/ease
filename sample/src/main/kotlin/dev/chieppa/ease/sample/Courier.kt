package dev.chieppa.ease.sample

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import dev.chieppa.ease.annotations.Doc
import dev.chieppa.ease.annotations.Range
import dev.chieppa.ease.ecs.replication.Replicated
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Doc("A courier: the cube its owner's input moves, at up to its pace.")
@Replicated
@Serializable
@SerialName("sample.Courier")
public class Courier : Component<Courier> {
  @Doc("How fast the courier moves at full input, in world units a second.")
  @Range(0.0, MAX_PACE)
  public var pace: Float = DEFAULT_PACE

  override fun type(): ComponentType<Courier> = Courier

  /** The component type, and the courier's paces. */
  public companion object : ComponentType<Courier>() {
    /** The pace a courier starts at, in world units a second. */
    public const val DEFAULT_PACE: Float = 4f

    /** The fastest pace a courier may be set to, in world units a second. */
    public const val MAX_PACE: Double = 12.0
  }
}

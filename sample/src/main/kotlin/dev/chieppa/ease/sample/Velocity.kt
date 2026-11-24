package dev.chieppa.ease.sample

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import dev.chieppa.ease.annotations.Doc
import dev.chieppa.ease.annotations.Range
import dev.chieppa.ease.ecs.replication.Replicated
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Doc("How fast an entity moves, in world units a second along x and y.")
@Replicated
@Serializable
@SerialName("sample.Velocity")
public class Velocity : Component<Velocity> {
  @Doc("Speed along x, in world units a second.")
  @Range(-MAX_SPEED, MAX_SPEED)
  public var x: Float = 0f

  @Doc("Speed along y, in world units a second.")
  @Range(-MAX_SPEED, MAX_SPEED)
  public var y: Float = 0f

  override fun type(): ComponentType<Velocity> = Velocity

  /** The component type, and a factory for a velocity. */
  public companion object : ComponentType<Velocity>() {
    /** The fastest an entity moves along one axis, in world units a second. */
    public const val MAX_SPEED: Double = 20.0

    /** A velocity of ([x], [y]) units a second. */
    public fun of(x: Float, y: Float): Velocity =
      Velocity().apply {
        this.x = x
        this.y = y
      }
  }
}

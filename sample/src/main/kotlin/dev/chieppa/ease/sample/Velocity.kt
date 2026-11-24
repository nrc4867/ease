package dev.chieppa.ease.sample

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import dev.chieppa.ease.ecs.replication.Replicated
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** How fast an entity moves, in world units a second along x and y. */
@Replicated
@Serializable
@SerialName("sample.Velocity")
public class Velocity : Component<Velocity> {
  public var x: Float = 0f
  public var y: Float = 0f

  override fun type(): ComponentType<Velocity> = Velocity

  /** The component type, and a factory for a velocity. */
  public companion object : ComponentType<Velocity>() {
    /** A velocity of ([x], [y]) units a second. */
    public fun of(x: Float, y: Float): Velocity =
      Velocity().apply {
        this.x = x
        this.y = y
      }
  }
}

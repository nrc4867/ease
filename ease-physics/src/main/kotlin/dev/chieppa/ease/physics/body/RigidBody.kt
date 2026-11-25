package dev.chieppa.ease.physics.body

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import dev.chieppa.ease.annotations.Doc
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Doc(
  "Makes a node's Collider a moving body. A world without authority over a dynamic body moves it " +
    "as a kinematic one, from the transform the authority sends."
)
@Serializable
@SerialName("ease.RigidBody")
public class RigidBody : Component<RigidBody> {
  @Doc("How the body moves.") public var motion: MotionKind = MotionKind.DYNAMIC

  override fun type(): ComponentType<RigidBody> = RigidBody

  /** The component type. */
  public companion object : ComponentType<RigidBody>()
}

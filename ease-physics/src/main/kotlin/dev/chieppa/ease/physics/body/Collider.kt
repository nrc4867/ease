package dev.chieppa.ease.physics.body

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import dev.chieppa.ease.annotations.Doc
import dev.chieppa.ease.annotations.Range
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Doc(
  "A solid shape at its node's transform. Without a RigidBody it is static: walls, floors and " +
    "blocking volumes. Its layer decides what it collides with, so a wall on a layer only the " +
    "player collides with stops the player and lets everyone else through; its surface type is " +
    "what raycasts and characters standing on it report. Nothing draws a collider."
)
@Serializable
@SerialName("ease.Collider")
public class Collider : Component<Collider> {
  @Doc("The shape.") public var shape: ShapeKind = ShapeKind.BOX

  @Doc("Half the box's size along x, in world units.")
  @Range(0.0, MAX_SIZE)
  public var halfX: Float = HALF_SIZE

  @Doc("Half the box's size along y, in world units.")
  @Range(0.0, MAX_SIZE)
  public var halfY: Float = HALF_SIZE

  @Doc("Half the box's size along z, in world units.")
  @Range(0.0, MAX_SIZE)
  public var halfZ: Float = HALF_SIZE

  @Doc("The sphere's or the capsule's radius, in world units.")
  @Range(0.0, MAX_SIZE)
  public var radius: Float = HALF_SIZE

  @Doc("Half the length of the capsule's straight part, in world units.")
  @Range(0.0, MAX_SIZE)
  public var halfHeight: Float = HALF_SIZE

  @Doc("The collision layer, by the name the game gave it; empty is the first layer.")
  public var layer: String = ""

  @Doc("The surface type, by the name the game gave it; empty is the default surface.")
  public var surface: String = ""

  override fun type(): ComponentType<Collider> = Collider

  /** The component type, and the shape's bounds. */
  public companion object : ComponentType<Collider>() {
    /** Half the size of a shape nothing sized, in world units. */
    public const val HALF_SIZE: Float = 0.5f

    /** The largest half size or radius, in world units. */
    public const val MAX_SIZE: Double = 10_000.0
  }
}

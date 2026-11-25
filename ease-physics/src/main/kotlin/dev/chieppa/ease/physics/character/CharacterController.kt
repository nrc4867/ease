package dev.chieppa.ease.physics.character

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import dev.chieppa.ease.annotations.Doc
import dev.chieppa.ease.annotations.Range
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Doc(
  "A character the CharacterMover moves: an upright capsule standing on its node's position, " +
    "which walks up slopes and steps, slides along walls and stands on what it lands on. Its " +
    "state is its node's LocalTransform and its CharacterMotion, which it must carry too."
)
@Serializable
@SerialName("ease.CharacterController")
public class CharacterController : Component<CharacterController> {
  @Doc("The capsule's radius, in world units.")
  @Range(MIN_SIZE, MAX_SIZE)
  public var radius: Float = DEFAULT_RADIUS

  @Doc("The capsule's full height, feet to head, in world units; at least twice its radius.")
  @Range(MIN_SIZE, MAX_SIZE)
  public var height: Float = DEFAULT_HEIGHT

  @Doc("The steepest slope it walks up, in degrees; steeper ground is a wall it slides off.")
  @Range(0.0, MAX_SLOPE)
  public var maxSlopeDegrees: Float = DEFAULT_SLOPE

  @Doc("The tallest step it walks up, in world units.")
  @Range(0.0, MAX_SIZE)
  public var stepHeight: Float = DEFAULT_STEP

  @Doc("How far it walks on the ground between footsteps, in world units.")
  @Range(MIN_SIZE, MAX_SIZE)
  public var strideLength: Float = DEFAULT_STRIDE

  @Doc("The collision layer it moves on, by name; empty is the first layer.")
  public var layer: String = ""

  override fun type(): ComponentType<CharacterController> = CharacterController

  /** The component type, and the defaults: a person-sized character. */
  public companion object : ComponentType<CharacterController>() {
    /** The default radius, in world units. */
    public const val DEFAULT_RADIUS: Float = 0.3f

    /** The default height, in world units. */
    public const val DEFAULT_HEIGHT: Float = 1.8f

    /** The default steepest walkable slope, in degrees. */
    public const val DEFAULT_SLOPE: Float = 45f

    /** The default tallest step, in world units. */
    public const val DEFAULT_STEP: Float = 0.4f

    /** The default stride, in world units. */
    public const val DEFAULT_STRIDE: Float = 0.8f

    /** The smallest size, in world units. */
    public const val MIN_SIZE: Double = 0.01

    /** The largest size, in world units. */
    public const val MAX_SIZE: Double = 100.0

    /** The steepest slope a character can be told to walk, in degrees. */
    public const val MAX_SLOPE: Double = 89.0
  }
}

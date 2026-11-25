package dev.chieppa.ease.render.scene

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import dev.chieppa.ease.annotations.Doc
import dev.chieppa.ease.annotations.Range
import dev.chieppa.ease.ecs.replication.Replicated
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Doc(
  "A perspective camera at its entity's LocalTransform, looking down the transform's -z with +y " +
    "up. The world is drawn through the camera of the lowest entity id."
)
@Replicated
@Serializable
@SerialName("ease.Camera")
public class Camera : Component<Camera> {
  @Doc("The vertical field of view, in degrees.")
  @Range(MIN_FIELD_OF_VIEW, MAX_FIELD_OF_VIEW)
  public var fieldOfView: Float = DEFAULT_FIELD_OF_VIEW

  @Doc("The nearest distance drawn, in world units, above 0.")
  @Range(MIN_DISTANCE, MAX_DISTANCE)
  public var near: Float = DEFAULT_NEAR

  @Doc("The farthest distance drawn, in world units, beyond near.")
  @Range(MIN_DISTANCE, MAX_DISTANCE)
  public var far: Float = DEFAULT_FAR

  override fun type(): ComponentType<Camera> = Camera

  /** The component type, and a factory for a camera. */
  public companion object : ComponentType<Camera>() {
    /** The field of view a camera has unless told otherwise, in degrees. */
    public const val DEFAULT_FIELD_OF_VIEW: Float = 60f

    /** The narrowest field of view a camera takes, in degrees. */
    public const val MIN_FIELD_OF_VIEW: Double = 1.0

    /** The widest field of view a camera takes, in degrees. */
    public const val MAX_FIELD_OF_VIEW: Double = 179.0

    /** The least near or far distance a scene file may hold. */
    public const val MIN_DISTANCE: Double = 0.0001

    /** The greatest near or far distance a scene file may hold. */
    public const val MAX_DISTANCE: Double = 1.0e7

    /** The near distance a camera has unless told otherwise. */
    public const val DEFAULT_NEAR: Float = 0.1f

    /** The far distance a camera has unless told otherwise. */
    public const val DEFAULT_FAR: Float = 500f

    /** A camera with a [fieldOfView] in degrees. */
    public fun of(fieldOfView: Float = DEFAULT_FIELD_OF_VIEW): Camera =
      Camera().also { it.fieldOfView = fieldOfView }
  }
}

package dev.chieppa.ease.ecs

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import dev.chieppa.ease.annotations.Doc
import dev.chieppa.ease.annotations.Range
import dev.chieppa.ease.ecs.replication.Interpolated
import dev.chieppa.ease.ecs.replication.Quantized
import dev.chieppa.ease.ecs.replication.Replicated
import dev.chieppa.ease.math.Quat
import dev.chieppa.ease.math.Transform
import dev.chieppa.ease.math.Vec3
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Doc(
  "An entity's position, rotation (a quaternion) and scale relative to its parent, as plain " +
    "floats. It replicates with the position interpolated, and the rotation quantized and " +
    "interpolated as one quaternion."
)
@Replicated
@Serializable
@SerialName("ease.LocalTransform")
public class LocalTransform : Component<LocalTransform> {
  @Doc("Position along x, in world units.") @Interpolated public var x: Float = 0f
  @Doc("Position along y, in world units.") @Interpolated public var y: Float = 0f
  @Doc("Position along z, in world units.") @Interpolated public var z: Float = 0f

  @Doc("Rotation quaternion, x component.")
  @Interpolated(rotation = ROTATION)
  @Quantized(ROTATION_BITS)
  @Range(-1.0, 1.0)
  public var rotationX: Float = 0f

  @Doc("Rotation quaternion, y component.")
  @Interpolated(rotation = ROTATION)
  @Quantized(ROTATION_BITS)
  @Range(-1.0, 1.0)
  public var rotationY: Float = 0f

  @Doc("Rotation quaternion, z component.")
  @Interpolated(rotation = ROTATION)
  @Quantized(ROTATION_BITS)
  @Range(-1.0, 1.0)
  public var rotationZ: Float = 0f

  @Doc("Rotation quaternion, w component; 1 with the other three 0 is no rotation.")
  @Interpolated(rotation = ROTATION)
  @Quantized(ROTATION_BITS)
  @Range(-1.0, 1.0)
  public var rotationW: Float = 1f

  @Doc("Scale along x; 1 leaves the size unchanged.") public var scaleX: Float = 1f
  @Doc("Scale along y; 1 leaves the size unchanged.") public var scaleY: Float = 1f
  @Doc("Scale along z; 1 leaves the size unchanged.") public var scaleZ: Float = 1f

  override fun type(): ComponentType<LocalTransform> = LocalTransform

  /** The transform as an immutable value. */
  public fun get(): Transform =
    Transform(
      Vec3(x, y, z),
      Quat(rotationX, rotationY, rotationZ, rotationW),
      Vec3(scaleX, scaleY, scaleZ),
    )

  /** Copies [transform] into this component. */
  public fun set(transform: Transform) {
    setTranslation(transform.translation())
    setRotation(transform.rotation())
    setScale(transform.scale())
  }

  private fun setTranslation(translation: Vec3) {
    x = translation.x()
    y = translation.y()
    z = translation.z()
  }

  private fun setRotation(rotation: Quat) {
    rotationX = rotation.x()
    rotationY = rotation.y()
    rotationZ = rotation.z()
    rotationW = rotation.w()
  }

  private fun setScale(scale: Vec3) {
    scaleX = scale.x()
    scaleY = scale.y()
    scaleZ = scale.z()
  }

  /** The component type, and a factory for a component holding [transform]. */
  public companion object : ComponentType<LocalTransform>() {
    /** Bits each rotation component takes on the wire. */
    public const val ROTATION_BITS: Int = 16

    /** The interpolation group of the rotation's four components. */
    public const val ROTATION: String = "rotation"

    /** A component holding [transform]. */
    public fun of(transform: Transform): LocalTransform = LocalTransform().apply { set(transform) }
  }
}

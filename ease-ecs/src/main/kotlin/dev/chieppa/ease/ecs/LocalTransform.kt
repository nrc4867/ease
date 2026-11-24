package dev.chieppa.ease.ecs

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import dev.chieppa.ease.math.Quat
import dev.chieppa.ease.math.Transform
import dev.chieppa.ease.math.Vec3
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * An entity's position, rotation and scale relative to its parent, as plain floats so systems can
 * update it in place. [get] and [set] convert to and from the immutable [Transform].
 */
@Serializable
@SerialName("ease.LocalTransform")
public class LocalTransform : Component<LocalTransform> {
  public var x: Float = 0f
  public var y: Float = 0f
  public var z: Float = 0f
  public var rotationX: Float = 0f
  public var rotationY: Float = 0f
  public var rotationZ: Float = 0f
  public var rotationW: Float = 1f
  public var scaleX: Float = 1f
  public var scaleY: Float = 1f
  public var scaleZ: Float = 1f

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
    /** A component holding [transform]. */
    public fun of(transform: Transform): LocalTransform = LocalTransform().apply { set(transform) }
  }
}

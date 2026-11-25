package dev.chieppa.ease.physics.jolt

import com.github.stephengold.joltjni.Quat
import com.github.stephengold.joltjni.RVec3
import dev.chieppa.ease.ecs.LocalTransform
import kotlin.math.sqrt

/**
 * Poses passed between `LocalTransform`s and Jolt: the reusable [position] and [rotation] Jolt's
 * calls read and fill, and, by entity id, the pose each body was last given, so a static body is
 * moved only when its transform changes. Nothing here allocates once grown.
 */
internal class BodyPoses(capacity: Int) {
  val position: RVec3 = RVec3()
  val rotation: Quat = Quat()
  private var given = FloatArray(capacity * POSE)

  /** Sets [position] and [rotation] from [transform], its rotation normalized. */
  fun load(transform: LocalTransform) {
    position.set(transform.x.toDouble(), transform.y.toDouble(), transform.z.toDouble())
    val x = transform.rotationX
    val y = transform.rotationY
    val z = transform.rotationZ
    val w = transform.rotationW
    val length = sqrt(x * x + y * y + z * z + w * w)
    if (length == 0f) rotation.set(0f, 0f, 0f, 1f)
    else rotation.set(x / length, y / length, z / length, w / length)
  }

  /** Copies [position] and [rotation] into [into], leaving its scale. */
  fun store(into: LocalTransform) {
    into.x = position.x()
    into.y = position.y()
    into.z = position.z()
    into.rotationX = rotation.x
    into.rotationY = rotation.y
    into.rotationZ = rotation.z
    into.rotationW = rotation.w
  }

  /** Whether [transform] differs from the pose body [id] was last given. */
  fun changed(id: Int, transform: LocalTransform): Boolean {
    var at = id * POSE
    return given[at++] != transform.x ||
      given[at++] != transform.y ||
      given[at++] != transform.z ||
      given[at++] != transform.rotationX ||
      given[at++] != transform.rotationY ||
      given[at++] != transform.rotationZ ||
      given[at] != transform.rotationW
  }

  /** Notes [transform] as the pose body [id] was given. */
  fun remember(id: Int, transform: LocalTransform) {
    var at = id * POSE
    given[at++] = transform.x
    given[at++] = transform.y
    given[at++] = transform.z
    given[at++] = transform.rotationX
    given[at++] = transform.rotationY
    given[at++] = transform.rotationZ
    given[at] = transform.rotationW
  }

  /** Makes room for [capacity] bodies. */
  fun grow(capacity: Int) {
    given = given.copyOf(capacity * POSE)
  }

  private companion object {
    /** Floats in a pose: a position and a quaternion. */
    const val POSE = 7
  }
}

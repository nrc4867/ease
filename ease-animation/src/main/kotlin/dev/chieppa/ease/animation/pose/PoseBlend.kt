package dev.chieppa.ease.animation.pose

import dev.chieppa.ease.math.Quats
import dev.chieppa.ease.scene.transform.TRANSFORM_ROTATION_AT
import dev.chieppa.ease.scene.transform.TRANSFORM_SCALE_AT
import dev.chieppa.ease.scene.transform.WORLD_TRANSFORM_FLOATS

/**
 * Blends whole poses joint by joint: positions and scales straight, rotations along the shortest
 * arc. A pose is [WORLD_TRANSFORM_FLOATS] floats a joint (position, rotation x, y, z, w, scale),
 * relative to the joint's parent. Blending allocates nothing.
 */
internal object PoseBlend {
  /**
   * Writes into [dst] the first [joints] joints of [from] moved [t] of the way to [to]: 0 is
   * [from], 1 is [to]. [dst] may be either input.
   */
  fun blend(from: FloatArray, to: FloatArray, t: Float, dst: FloatArray, joints: Int) {
    for (joint in 0 until joints) {
      val at = joint * WORLD_TRANSFORM_FLOATS
      for (axis in 0 until TRANSFORM_ROTATION_AT) dst[at + axis] = lerp(from, to, at + axis, t)
      for (axis in TRANSFORM_SCALE_AT until WORLD_TRANSFORM_FLOATS) {
        dst[at + axis] = lerp(from, to, at + axis, t)
      }
      Quats.slerp(
        from,
        at + TRANSFORM_ROTATION_AT,
        to,
        at + TRANSFORM_ROTATION_AT,
        t,
        dst,
        at + TRANSFORM_ROTATION_AT,
      )
    }
  }

  private fun lerp(from: FloatArray, to: FloatArray, at: Int, t: Float): Float =
    from[at] + (to[at] - from[at]) * t
}

package dev.chieppa.ease.scene.transform

import dev.chieppa.ease.math.Quats

private const val AXES = 3

/**
 * Composes transforms laid out as position, rotation (x, y, z, w) and scale floats, with scratch
 * made once, so composing allocates nothing. Not shared between threads.
 */
public class TransformMath {
  private val point = FloatArray(AXES)

  /**
   * Writes into [dst] at [d] the transform at [c] in [child] placed by the one at [p] in [parent]:
   * the child's position scaled, rotated and moved by the parent, rotations multiplied (parent
   * first in the product), scales multiplied axis by axis. [dst] may be [child] at the same offset.
   */
  @Suppress("LongParameterList") // Arrays and offsets, so composing allocates nothing.
  public fun compose(
    parent: FloatArray,
    p: Int,
    child: FloatArray,
    c: Int,
    dst: FloatArray,
    d: Int,
  ) {
    for (axis in 0 until AXES) point[axis] = parent[p + TRANSFORM_SCALE_AT + axis] * child[c + axis]
    Quats.rotate(parent, p + TRANSFORM_ROTATION_AT, point, 0, point, 0)
    for (axis in 0 until AXES) {
      dst[d + TRANSFORM_SCALE_AT + axis] =
        parent[p + TRANSFORM_SCALE_AT + axis] * child[c + TRANSFORM_SCALE_AT + axis]
    }
    Quats.mul(
      parent,
      p + TRANSFORM_ROTATION_AT,
      child,
      c + TRANSFORM_ROTATION_AT,
      dst,
      d + TRANSFORM_ROTATION_AT,
    )
    for (axis in 0 until AXES) dst[d + axis] = parent[p + axis] + point[axis]
  }
}

package dev.chieppa.ease.scene.transform

import dev.chieppa.ease.math.Quats

private const val ROTATION_AT = 3
private const val SCALE_AT = 7
private const val AXES = 3

/**
 * Composes transforms laid out as position, rotation (x, y, z, w) and scale floats, with scratch
 * made once, so composing allocates nothing. Not shared between threads.
 */
internal class TransformMath {
  private val point = FloatArray(AXES)

  /**
   * Writes into [dst] at [d] the transform at [c] in [child] placed by the one at [p] in [parent]:
   * the child's position scaled, rotated and moved by the parent, rotations multiplied (parent
   * first in the product), scales multiplied axis by axis. [dst] may be [child] at the same offset.
   */
  @Suppress("LongParameterList") // Arrays and offsets, so composing allocates nothing.
  fun compose(parent: FloatArray, p: Int, child: FloatArray, c: Int, dst: FloatArray, d: Int) {
    for (axis in 0 until AXES) point[axis] = parent[p + SCALE_AT + axis] * child[c + axis]
    Quats.rotate(parent, p + ROTATION_AT, point, 0, point, 0)
    for (axis in 0 until AXES) {
      dst[d + SCALE_AT + axis] = parent[p + SCALE_AT + axis] * child[c + SCALE_AT + axis]
    }
    Quats.mul(parent, p + ROTATION_AT, child, c + ROTATION_AT, dst, d + ROTATION_AT)
    for (axis in 0 until AXES) dst[d + axis] = parent[p + axis] + point[axis]
  }
}

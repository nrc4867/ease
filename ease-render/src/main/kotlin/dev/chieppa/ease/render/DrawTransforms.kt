package dev.chieppa.ease.render

import dev.chieppa.ease.math.Mat4s

private const val MATRIX = 16
private const val AXES = 3

/**
 * Turns a transform into a draw's model matrix and normal matrix (the rotation with the scale
 * inverted, which keeps normals perpendicular under any scale), in scratch arrays made once.
 */
internal class DrawTransforms {
  private val origin = FloatArray(3)
  private val inverseScale = FloatArray(3)

  /**
   * Writes the matrices of [transform] ([TRANSFORM_FLOATS] floats) at draw [index] of [models] and
   * [normals].
   */
  fun write(transform: FloatArray, models: FloatArray, normals: FloatArray, index: Int) {
    val at = index * MATRIX
    Mat4s.fromTransform(transform, 0, transform, ROTATION_AT, transform, SCALE_AT, models, at)
    for (axis in 0 until AXES) {
      val scale = transform[SCALE_AT + axis]
      inverseScale[axis] = if (scale == 0f) 0f else 1f / scale
    }
    Mat4s.fromTransform(origin, 0, transform, ROTATION_AT, inverseScale, 0, normals, at)
  }
}

package dev.chieppa.ease.gltf

import dev.chieppa.ease.gltf.json.NodeJson
import kotlin.math.sqrt

/**
 * A node's transform relative to its parent as translation, rotation (x, y, z, w) and scale. A node
 * given by matrix is split into those parts; a negative determinant flips the x scale.
 */
public class NodeTransform(
  public val translation: FloatArray,
  public val rotation: FloatArray,
  public val scale: FloatArray,
) {
  public companion object {
    private const val MATRIX_FLOATS = 16
    private const val QUARTER = 0.25f
    private const val TRANSLATION_AT = 12
    private const val AXES = 3
    private const val QUAT = 4
    private const val W = 3

    /** The transform of node [index], whose JSON is [node]. */
    public fun of(node: NodeJson, index: Int): NodeTransform {
      val path = "nodes[$index]"
      node.matrix?.let { matrix ->
        if (matrix.size != MATRIX_FLOATS) fail("$path.matrix", "needs 16 numbers")
        return fromMatrix(matrix.toFloatArray())
      }
      return NodeTransform(
        sized(node.translation, AXES, 0f, "$path.translation"),
        sized(node.rotation, QUAT, 0f, "$path.rotation").also {
          if (node.rotation == null) it[W] = 1f
        },
        sized(node.scale, AXES, 1f, "$path.scale"),
      )
    }

    private fun sized(values: List<Float>?, size: Int, fill: Float, path: String): FloatArray {
      if (values == null) return FloatArray(size) { fill }
      if (values.size != size) fail(path, "needs $size numbers, got ${values.size}")
      return values.toFloatArray()
    }

    /** Splits a column-major affine [m] into translation, rotation and scale. */
    private fun fromMatrix(m: FloatArray): NodeTransform {
      val translation =
        floatArrayOf(m[TRANSLATION_AT], m[TRANSLATION_AT + 1], m[TRANSLATION_AT + 2])
      val scale = FloatArray(AXES) { column -> length(m, column * QUAT) }
      if (determinant(m) < 0) scale[0] = -scale[0]
      val r = FloatArray(MATRIX_FLOATS)
      for (column in 0 until AXES) {
        val s = scale[column].takeIf { it != 0f } ?: 1f
        for (row in 0 until AXES) r[column * QUAT + row] = m[column * QUAT + row] / s
      }
      return NodeTransform(translation, quaternion(r), scale)
    }

    private fun length(m: FloatArray, at: Int): Float =
      sqrt(m[at] * m[at] + m[at + 1] * m[at + 1] + m[at + 2] * m[at + 2])

    @Suppress("MagicNumber") // Matrix entries by their column-major index.
    private fun determinant(m: FloatArray): Float =
      m[0] * (m[5] * m[10] - m[9] * m[6]) - m[4] * (m[1] * m[10] - m[9] * m[2]) +
        m[8] * (m[1] * m[6] - m[5] * m[2])

    /** The unit quaternion of the pure rotation [r], column-major. */
    @Suppress("MagicNumber") // Matrix entries by their column-major index.
    private fun quaternion(r: FloatArray): FloatArray {
      val m00 = r[0]
      val m11 = r[5]
      val m22 = r[10]
      val trace = m00 + m11 + m22
      val q = FloatArray(QUAT)
      if (trace > 0) {
        val s = sqrt(trace + 1f) * 2f
        q[3] = QUARTER * s
        q[0] = (r[6] - r[9]) / s
        q[1] = (r[8] - r[2]) / s
        q[2] = (r[1] - r[4]) / s
      } else if (m00 > m11 && m00 > m22) {
        val s = sqrt(1f + m00 - m11 - m22) * 2f
        q[3] = (r[6] - r[9]) / s
        q[0] = QUARTER * s
        q[1] = (r[4] + r[1]) / s
        q[2] = (r[8] + r[2]) / s
      } else if (m11 > m22) {
        val s = sqrt(1f + m11 - m00 - m22) * 2f
        q[3] = (r[8] - r[2]) / s
        q[0] = (r[4] + r[1]) / s
        q[1] = QUARTER * s
        q[2] = (r[9] + r[6]) / s
      } else {
        val s = sqrt(1f + m22 - m00 - m11) * 2f
        q[3] = (r[1] - r[4]) / s
        q[0] = (r[8] + r[2]) / s
        q[1] = (r[9] + r[6]) / s
        q[2] = QUARTER * s
      }
      return q
    }
  }
}

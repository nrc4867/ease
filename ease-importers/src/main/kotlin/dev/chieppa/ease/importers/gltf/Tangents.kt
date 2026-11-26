package dev.chieppa.ease.importers.gltf

import kotlin.math.abs
import kotlin.math.sqrt

private const val VEC2 = 2
private const val VEC3 = 3
private const val VEC4 = 4
private const val TRIANGLE = 3
private const val TINY = 1e-12f

/**
 * Normals and tangents a glTF file leaves out, worked out from its triangles: normals as the
 * area-weighted sum of the faces around each vertex, tangents along each face's +u direction,
 * summed per vertex and made perpendicular to the normal, with the handedness of +v in w.
 */
internal object Tangents {
  /** Unit normals of [positions] from the triangles of [indices]. */
  fun normals(positions: FloatArray, indices: IntArray): FloatArray {
    val normals = FloatArray(positions.size)
    for (t in indices.indices step TRIANGLE) {
      val a = indices[t] * VEC3
      val b = indices[t + 1] * VEC3
      val c = indices[t + 2] * VEC3
      val e1 = FloatArray(VEC3) { positions[b + it] - positions[a + it] }
      val e2 = FloatArray(VEC3) { positions[c + it] - positions[a + it] }
      val face = cross(e1, e2)
      for (vertex in intArrayOf(a, b, c)) for (i in 0 until VEC3) normals[vertex + i] += face[i]
    }
    for (v in 0 until normals.size / VEC3) normalize(normals, v * VEC3, fallbackY = true)
    return normals
  }

  /** Tangents with handedness, four floats a vertex, from [uv] and the triangles of [indices]. */
  fun fromUv(
    positions: FloatArray,
    normals: FloatArray,
    uv: FloatArray,
    indices: IntArray,
  ): FloatArray {
    val count = positions.size / VEC3
    val tangent = FloatArray(count * VEC3)
    val bitangent = FloatArray(count * VEC3)
    for (t in indices.indices step TRIANGLE) {
      val v = intArrayOf(indices[t], indices[t + 1], indices[t + 2])
      val e1 = FloatArray(VEC3) { positions[v[1] * VEC3 + it] - positions[v[0] * VEC3 + it] }
      val e2 = FloatArray(VEC3) { positions[v[2] * VEC3 + it] - positions[v[0] * VEC3 + it] }
      val du1 = uv[v[1] * VEC2] - uv[v[0] * VEC2]
      val dv1 = uv[v[1] * VEC2 + 1] - uv[v[0] * VEC2 + 1]
      val du2 = uv[v[2] * VEC2] - uv[v[0] * VEC2]
      val dv2 = uv[v[2] * VEC2 + 1] - uv[v[0] * VEC2 + 1]
      val det = du1 * dv2 - du2 * dv1
      if (abs(det) < TINY) continue
      val r = 1f / det
      for (vertex in v) {
        for (i in 0 until VEC3) {
          tangent[vertex * VEC3 + i] += (e1[i] * dv2 - e2[i] * dv1) * r
          bitangent[vertex * VEC3 + i] += (e2[i] * du1 - e1[i] * du2) * r
        }
      }
    }
    return FloatArray(count * VEC4).also { out -> orthonormalize(normals, tangent, bitangent, out) }
  }

  private fun orthonormalize(n: FloatArray, t: FloatArray, b: FloatArray, out: FloatArray) {
    for (v in 0 until n.size / VEC3) {
      val at = v * VEC3
      val dot = n[at] * t[at] + n[at + 1] * t[at + 1] + n[at + 2] * t[at + 2]
      for (i in 0 until VEC3) out[v * VEC4 + i] = t[at + i] - n[at + i] * dot
      if (!normalize(out, v * VEC4, fallbackY = false)) perpendicular(n, at, out, v * VEC4)
      val nv = floatArrayOf(n[at], n[at + 1], n[at + 2])
      val tv = floatArrayOf(out[v * VEC4], out[v * VEC4 + 1], out[v * VEC4 + 2])
      val c = cross(nv, tv)
      val hand = c[0] * b[at] + c[1] * b[at + 1] + c[2] * b[at + 2]
      out[v * VEC4 + VEC3] = if (hand < 0f) -1f else 1f
    }
  }

  /** Any unit vector perpendicular to the normal at [at], written at [to]. */
  private fun perpendicular(n: FloatArray, at: Int, out: FloatArray, to: Int) {
    val axis = if (abs(n[at]) < 0.9f) floatArrayOf(1f, 0f, 0f) else floatArrayOf(0f, 1f, 0f)
    val c = cross(floatArrayOf(n[at], n[at + 1], n[at + 2]), axis)
    for (i in 0 until VEC3) out[to + i] = c[i]
    normalize(out, to, fallbackY = false)
  }

  private fun cross(a: FloatArray, b: FloatArray) =
    floatArrayOf(a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0])

  /** Scales the vector at [at] to length 1; a zero vector becomes +y or stays, as asked. */
  private fun normalize(values: FloatArray, at: Int, fallbackY: Boolean): Boolean {
    val length =
      sqrt(
        values[at] * values[at] + values[at + 1] * values[at + 1] + values[at + 2] * values[at + 2]
      )
    if (length < TINY) {
      if (fallbackY) {
        values[at] = 0f
        values[at + 1] = 1f
        values[at + 2] = 0f
      }
      return false
    }
    for (i in 0 until VEC3) values[at + i] /= length
    return true
  }
}

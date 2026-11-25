package dev.chieppa.ease.sdf.field

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * The distance functions in Kotlin. Each mirrors its WGSL twin in `sdf.wgsl`, named with `sdf_` in
 * front, operation for operation, so the CPU and the GPU agree to within rounding. Only correctly
 * rounded operations are used, so every JVM gives the same bits.
 */
@Suppress("TooManyFunctions") // One function per primitive and join, as in the WGSL.
internal object SdfMath {
  fun length(x: Float, y: Float, z: Float): Float = sqrt(x * x + y * y + z * z)

  fun length(x: Float, y: Float): Float = sqrt(x * x + y * y)

  fun sphere(x: Float, y: Float, z: Float, radius: Float): Float = length(x, y, z) - radius

  @Suppress("LongParameterList")
  fun box(x: Float, y: Float, z: Float, halfX: Float, halfY: Float, halfZ: Float): Float {
    val qx = abs(x) - halfX
    val qy = abs(y) - halfY
    val qz = abs(z) - halfZ
    val outside = length(maxOf(qx, 0f), maxOf(qy, 0f), maxOf(qz, 0f))
    val inside = minOf(maxOf(qx, maxOf(qy, qz)), 0f)
    return outside + inside
  }

  @Suppress("LongParameterList")
  fun roundedBox(
    x: Float,
    y: Float,
    z: Float,
    halfX: Float,
    halfY: Float,
    halfZ: Float,
    radius: Float,
  ): Float = box(x, y, z, halfX - radius, halfY - radius, halfZ - radius) - radius

  fun capsule(x: Float, y: Float, z: Float, halfHeight: Float, radius: Float): Float {
    val along = y - y.coerceIn(-halfHeight, halfHeight)
    return length(x, along, z) - radius
  }

  fun cylinder(x: Float, y: Float, z: Float, radius: Float, halfHeight: Float): Float {
    val dx = length(x, z) - radius
    val dy = abs(y) - halfHeight
    return minOf(maxOf(dx, dy), 0f) + length(maxOf(dx, 0f), maxOf(dy, 0f))
  }

  @Suppress("LongParameterList")
  fun plane(x: Float, y: Float, z: Float, nx: Float, ny: Float, nz: Float, offset: Float): Float =
    x * nx + y * ny + z * nz - offset

  fun torus(x: Float, y: Float, z: Float, major: Float, minor: Float): Float =
    length(length(x, z) - major, y) - minor

  fun union(a: Float, b: Float, blend: Float): Float {
    if (blend <= 0f) return minOf(a, b)
    val h = (HALF + HALF * (b - a) / blend).coerceIn(0f, 1f)
    return b + (a - b) * h - blend * h * (1f - h)
  }

  fun subtract(a: Float, b: Float, blend: Float): Float {
    if (blend <= 0f) return maxOf(a, -b)
    val h = (HALF - HALF * (a + b) / blend).coerceIn(0f, 1f)
    return a + (-b - a) * h + blend * h * (1f - h)
  }

  fun intersect(a: Float, b: Float, blend: Float): Float {
    if (blend <= 0f) return maxOf(a, b)
    val h = (HALF - HALF * (b - a) / blend).coerceIn(0f, 1f)
    return b + (a - b) * h + blend * h * (1f - h)
  }

  private const val HALF = 0.5f
}

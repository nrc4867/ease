package dev.chieppa.ease.sdf

import dev.chieppa.ease.math.Quat
import dev.chieppa.ease.sdf.field.DistanceField
import dev.chieppa.ease.sdf.shape.Box
import dev.chieppa.ease.sdf.shape.Capsule
import dev.chieppa.ease.sdf.shape.Cylinder
import dev.chieppa.ease.sdf.shape.Plane
import dev.chieppa.ease.sdf.shape.RoundedBox
import dev.chieppa.ease.sdf.shape.SdfShape
import dev.chieppa.ease.sdf.shape.Sphere
import dev.chieppa.ease.sdf.shape.Torus
import dev.chieppa.ease.sdf.shape.intersect
import dev.chieppa.ease.sdf.shape.moved
import dev.chieppa.ease.sdf.shape.rotated
import dev.chieppa.ease.sdf.shape.scaled
import dev.chieppa.ease.sdf.shape.subtract
import dev.chieppa.ease.sdf.shape.union
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Primitives, transforms and joins give the distances their geometry says. */
class DistanceFieldTest {
  @Test
  fun `primitives measure from their surfaces`() {
    assertDistance(1f, Sphere(2f), 3f, 0f, 0f)
    assertDistance(-0.5f, Box(1f, 2f, 3f), 0.5f, 0f, 0f)
    assertDistance(5f, Box(1f, 1f, 1f), 1f, 1f, 6f)
    assertDistance(1f, RoundedBox(1f, 1f, 1f, 0.5f), 2f, 0f, 0f)
    assertDistance(0.5f, Capsule(1f, 0.5f), 0f, 2f, 0f)
    assertDistance(2f, Capsule(1f, 0.5f), 2.5f, 0.5f, 0f)
    assertDistance(1f, Cylinder(1f, 2f), 0f, 3f, 0f)
    assertDistance(-1f, Plane(0f, 2f, 0f, 1f), 5f, 0f, -7f)
    assertDistance(0.5f, Torus(3f, 0.5f), 3f, 1f, 0f)
  }

  @Test
  fun `transforms move, turn and scale a shape`() {
    assertDistance(0f, Sphere(1f).moved(5f, 0f, 0f), 6f, 0f, 0f)
    val lying = Capsule(2f, 0.5f).rotated(Quat(0f, 0f, QUARTER, QUARTER))
    assertDistance(0f, lying, 2.5f, 0f, 0f)
    assertDistance(0f, Sphere(1f).scaled(3f).moved(1f, 0f, 0f), 4f, 0f, 0f)
    assertDistance(1f, Box(1f, 1f, 1f).moved(2f, 0f, 0f).scaled(2f), 7f, 0f, 0f)
  }

  @Test
  fun `hard joins are minimum and maximum, blends round the seam`() {
    val a = Sphere(1f)
    val b = Sphere(1f).moved(1.5f, 0f, 0f)
    assertDistance(-1f, a.union(b), 0f, 0f, 0f)
    assertDistance(0.5f, a.subtract(b), 1f, 0f, 0f)
    assertDistance(0.5f, a.intersect(b), 0f, 0f, 0f)
    val seam = DistanceField(a.union(b)).distance(0.75f, 0.9f, 0f)
    val blended = DistanceField(a.union(b, blend = 0.5f)).distance(0.75f, 0.9f, 0f)
    assertTrue(blended < seam, "a blend fills the seam: $blended against $seam")
  }

  @Test
  fun `the same tree gives the same distances, bit for bit`() {
    val first = DistanceField(TREE)
    val second = DistanceField(TREE)
    for (step in 0 until SAMPLES) {
      val x = step * 0.37f - 15f
      val y = step * 0.11f - 6f
      val z = step * 0.23f - 11f
      assertEquals(first.distance(x, y, z).toRawBits(), second.distance(x, y, z).toRawBits())
    }
  }

  private fun assertDistance(expected: Float, shape: SdfShape, x: Float, y: Float, z: Float) {
    assertEquals(expected, DistanceField(shape).distance(x, y, z), TOLERANCE)
  }

  private companion object {
    /** A block with a blended pair of tunnels carved through it, turned and scaled. */
    val TREE: SdfShape =
      Box(8f, 4f, 6f)
        .subtract(
          Capsule(6f, 1.5f)
            .rotated(Quat(0f, 0f, 0.70710677f, 0.70710677f))
            .union(Sphere(2.5f), 0.8f),
          blend = 0.4f,
        )
        .rotated(Quat(0f, 0.38268343f, 0f, 0.9238795f))
        .scaled(1.3f)

    const val QUARTER = 0.70710677f
    const val TOLERANCE = 1.0e-5f
    const val SAMPLES = 100
  }
}

package dev.chieppa.ease.sdf.gpu

import dev.chieppa.ease.sdf.mesh.SurfaceNets
import kotlin.test.Test
import kotlin.test.assertEquals

/** The cave's collider triangles are the same, bit for bit, every time it is built. */
class SdfColliderTest {
  @Test
  fun `the same tree gives the same collider triangles`() {
    val first = SurfaceNets.mesh(TestCave.field(), TestCave.grid()).triangleMesh()
    val second = SurfaceNets.mesh(TestCave.field(), TestCave.grid()).triangleMesh()

    assertEquals(first.triangleCount, second.triangleCount)
    for (triangle in 0 until first.triangleCount) {
      for (corner in 0 until 3) {
        val vertex = first.corner(triangle, corner)
        assertEquals(vertex, second.corner(triangle, corner))
        for (axis in 0 until 3) {
          assertEquals(
            first.position(vertex, axis).toRawBits(),
            second.position(vertex, axis).toRawBits(),
          )
        }
      }
    }
  }
}

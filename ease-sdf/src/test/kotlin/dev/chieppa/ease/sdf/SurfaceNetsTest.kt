package dev.chieppa.ease.sdf

import dev.chieppa.ease.sdf.field.DistanceField
import dev.chieppa.ease.sdf.mesh.SdfGrid
import dev.chieppa.ease.sdf.mesh.SdfMesh
import dev.chieppa.ease.sdf.mesh.SurfaceNets
import dev.chieppa.ease.sdf.shape.Sphere
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** CPU surface nets: a closed, outward-facing surface on the field's zero, the same every run. */
class SurfaceNetsTest {
  private val sphere = DistanceField(Sphere(RADIUS))
  private val grid = SdfGrid.around(-RADIUS, -RADIUS, -RADIUS, RADIUS, RADIUS, RADIUS, CELL)

  @Test
  fun `a sphere's vertices lie on it and its normals point out`() {
    val mesh = SurfaceNets.mesh(sphere, grid)

    assertTrue(mesh.triangleCount > 100, "${mesh.triangleCount} triangles")
    for (vertex in 0 until mesh.vertexCount) {
      val x = mesh.positions[vertex * 3]
      val y = mesh.positions[vertex * 3 + 1]
      val z = mesh.positions[vertex * 3 + 2]
      val distance = sqrt(x * x + y * y + z * z)
      assertEquals(RADIUS, distance, CELL * 0.25f, "vertex $vertex")
      val outward =
        (x * mesh.normals[vertex * 3] +
          y * mesh.normals[vertex * 3 + 1] +
          z * mesh.normals[vertex * 3 + 2]) / distance
      assertTrue(outward > 0.95f, "normal of vertex $vertex leans $outward")
    }
  }

  @Test
  fun `every triangle runs counter-clockwise seen from outside`() {
    val mesh = SurfaceNets.mesh(sphere, grid)

    for (triangle in 0 until mesh.triangleCount) {
      val facing = facing(mesh, triangle)
      assertTrue(facing > 0f, "triangle $triangle faces in ($facing)")
    }
  }

  @Test
  fun `a closed surface uses every edge twice, once each way`() {
    val mesh = SurfaceNets.mesh(sphere, grid)
    val edges = HashMap<Long, Int>()
    for (triangle in 0 until mesh.triangleCount) {
      for (corner in 0 until 3) {
        val from = mesh.indices[triangle * 3 + corner]
        val to = mesh.indices[triangle * 3 + (corner + 1) % 3]
        edges.merge(from.toLong() shl 32 or to.toLong(), 1, Int::plus)
      }
    }

    for ((edge, count) in edges) {
      val reverse = (edge and 0xFFFFFFFFL) shl 32 or (edge ushr 32)
      assertEquals(1, count, "edge $edge")
      assertEquals(1, edges[reverse], "edge $edge has no twin")
    }
  }

  @Test
  fun `the same tree and grid give the same mesh bytes`() {
    val first = SurfaceNets.mesh(TestCave.field(), TestCave.grid())
    val second = SurfaceNets.mesh(TestCave.field(), TestCave.grid())

    assertContentEquals(
      first.positions.map(Float::toRawBits),
      second.positions.map(Float::toRawBits),
    )
    assertContentEquals(first.normals.map(Float::toRawBits), second.normals.map(Float::toRawBits))
    assertContentEquals(first.indices, second.indices)
    val colliderA = first.triangleMesh()
    val colliderB = second.triangleMesh()
    assertEquals(colliderA.triangleCount, colliderB.triangleCount)
    for (triangle in 0 until colliderA.triangleCount) {
      for (corner in 0 until 3) {
        val vertex = colliderA.corner(triangle, corner)
        assertEquals(vertex, colliderB.corner(triangle, corner))
        for (axis in 0 until 3) {
          assertEquals(
            colliderA.position(vertex, axis).toRawBits(),
            colliderB.position(vertex, axis).toRawBits(),
          )
        }
      }
    }
  }

  /** How far [triangle]'s front normal points away from the sphere's center. */
  private fun facing(mesh: SdfMesh, triangle: Int): Float {
    fun at(corner: Int, axis: Int) = mesh.positions[mesh.indices[triangle * 3 + corner] * 3 + axis]
    val ux = at(1, 0) - at(0, 0)
    val uy = at(1, 1) - at(0, 1)
    val uz = at(1, 2) - at(0, 2)
    val vx = at(2, 0) - at(0, 0)
    val vy = at(2, 1) - at(0, 1)
    val vz = at(2, 2) - at(0, 2)
    val nx = uy * vz - uz * vy
    val ny = uz * vx - ux * vz
    val nz = ux * vy - uy * vx
    val cx = at(0, 0) + at(1, 0) + at(2, 0)
    val cy = at(0, 1) + at(1, 1) + at(2, 1)
    val cz = at(0, 2) + at(1, 2) + at(2, 2)
    return if (abs(nx) + abs(ny) + abs(nz) == 0f) 1f else nx * cx + ny * cy + nz * cz
  }

  private companion object {
    const val RADIUS = 2f
    const val CELL = 0.25f
  }
}

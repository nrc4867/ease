package dev.chieppa.ease.sdf.gpu

import dev.chieppa.ease.gpu.GpuContext
import dev.chieppa.ease.gpu.mesh.ComputedMesh
import dev.chieppa.ease.gpu.mesh.VERTEX_BYTES
import dev.chieppa.ease.math.Quat
import dev.chieppa.ease.sdf.field.DistanceField
import dev.chieppa.ease.sdf.mesh.SdfGrid
import dev.chieppa.ease.sdf.mesh.SurfaceNets
import dev.chieppa.ease.sdf.shape.Box
import dev.chieppa.ease.sdf.shape.Capsule
import dev.chieppa.ease.sdf.shape.Cylinder
import dev.chieppa.ease.sdf.shape.Plane
import dev.chieppa.ease.sdf.shape.RoundedBox
import dev.chieppa.ease.sdf.shape.Sphere
import dev.chieppa.ease.sdf.shape.Torus
import dev.chieppa.ease.sdf.shape.intersect
import dev.chieppa.ease.sdf.shape.moved
import dev.chieppa.ease.sdf.shape.rotated
import dev.chieppa.ease.sdf.shape.scaled
import dev.chieppa.ease.sdf.shape.subtract
import dev.chieppa.ease.sdf.shape.union
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertTrue

/**
 * The GPU meshes a tree as the CPU does, at the same places to within rounding, for every
 * primitive, transform and join. Only next to corners within rounding of the surface may another
 * GPU put a corner on the other side; everywhere else vertices, normals and triangles match, and
 * with no such corner the triangles match index for index.
 */
class GpuMeshingGpuTest {
  @Test
  fun `every primitive, transform and join meshes alike on the GPU and the CPU`() {
    val shape =
      Sphere(1.2f)
        .union(Box(0.8f, 0.5f, 0.6f).moved(1.6f, 0.1f, 0.2f), blend = 0.3f)
        .union(RoundedBox(0.7f, 0.7f, 0.5f, 0.2f).moved(-1.7f, 0.3f, -0.1f))
        .union(
          Capsule(0.6f, 0.35f).rotated(Quat(0f, 0f, 0.38268343f, 0.9238795f)).moved(0f, 1.6f, 0f)
        )
        .union(Torus(0.9f, 0.25f).scaled(1.1f).moved(0.1f, -1.4f, 0.3f), blend = 0.2f)
        .subtract(Cylinder(0.4f, 3f).moved(0.05f, 0f, 0.07f), blend = 0.15f)
        .intersect(Plane(0.1f, 0.2f, 1f, 0.9f), blend = 0.1f)
    val grid = SdfGrid.around(-2.7f, -2.1f, -1.6f, 2.6f, 2.3f, 1.5f, 0.13f)

    assertAlike(DistanceField(shape), grid)
  }

  @Test
  fun `the cave meshes alike on the GPU and the CPU`() {
    assertAlike(TestCave.field(), TestCave.grid())
  }

  private fun assertAlike(field: DistanceField, grid: SdfGrid) {
    val cpu = SurfaceNets.mesh(field, grid)
    GpuContext.open().use { gpu ->
      GpuSurfaceNets.mesh(gpu, field, grid).use { mesh ->
        assertTrue(mesh.indexCount > 0)
        val vertices = floats(mesh.vertices.read(mesh.vertexCount * VERTEX_BYTES))
        val positions = FloatArray(mesh.vertexCount * 3)
        val normals = FloatArray(mesh.vertexCount * 3)
        for (vertex in 0 until mesh.vertexCount) {
          val at = vertex * ComputedMesh.VERTEX_FLOATS
          vertices.copyInto(positions, vertex * 3, at, at + 3)
          vertices.copyInto(normals, vertex * 3, at + NORMAL_AT, at + NORMAL_AT + 3)
        }
        val indices = ints(mesh.indices.read(mesh.indexCount * Int.SIZE_BYTES.toLong()))
        val match = MeshMatch(field, grid, cpu, AMBIGUITY)
        val problems =
          match.problems(positions, normals, indices, POSITION_TOLERANCE, NORMAL_TOLERANCE)
        assertTrue(problems.isEmpty(), problems.take(SHOWN).joinToString("; "))
        if (match.clear) assertContentEquals(cpu.indices, indices)
      }
    }
  }

  private fun floats(bytes: ByteArray): FloatArray {
    val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer()
    return FloatArray(buffer.remaining()).also { buffer.get(it) }
  }

  private fun ints(bytes: ByteArray): IntArray {
    val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer()
    return IntArray(buffer.remaining()).also { buffer.get(it) }
  }

  private companion object {
    const val NORMAL_AT = 3
    const val POSITION_TOLERANCE = 1.0e-4f
    const val NORMAL_TOLERANCE = 2.0e-3f
    const val AMBIGUITY = 1.0e-4f
    const val SHOWN = 10
  }
}

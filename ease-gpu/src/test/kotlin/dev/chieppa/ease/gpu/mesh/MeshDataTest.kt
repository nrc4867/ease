package dev.chieppa.ease.gpu.mesh

import dev.chieppa.ease.testing.AllocationBudget
import java.lang.foreign.ValueLayout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** Meshes built in native memory through the dynamic mesh API. */
class MeshDataTest {
  @Test
  fun `vertices and indices land in the GPU's layout, growing past the starting room`() {
    MeshData(vertexCapacity = 1, indexCapacity = 3).use { mesh ->
      val first = mesh.vertex(1f, 2f, 3f, 0f, 0f, 1f, 0.25f, 0.75f)
      mesh.vertex(0f, 0f, 0f, 0f, 0f, 1f, 0f, 0f)
      mesh.vertex(0f, 1f, 0f, 0f, 0f, 1f, 0f, 1f)
      mesh.triangle(first, 1, 2)
      mesh.triangle(2, 1, first)

      assertEquals(3, mesh.vertexCount)
      assertEquals(6, mesh.indexCount)
      assertEquals(3 * VERTEX_BYTES, mesh.vertexBytes)
      val floats = { at: Long -> mesh.vertices.get(ValueLayout.JAVA_FLOAT, at) }
      assertEquals(listOf(1f, 2f, 3f), listOf(0L, 4L, 8L).map(floats))
      assertEquals(listOf(0.25f, 0.75f), listOf(UV_OFFSET, UV_OFFSET + 4).map(floats))
      assertEquals(2, mesh.indices.getAtIndex(ValueLayout.JAVA_INT, 3))
    }
  }

  @Test
  fun `committing moves the version on, and a triangle must name existing vertices`() {
    MeshData().use { mesh ->
      mesh.vertex(0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f)
      mesh.commit()

      assertEquals(1L, mesh.version)
      assertFailsWith<IllegalArgumentException> { mesh.triangle(0, 0, 1) }
    }
  }

  @Test
  fun `rebuilding a mesh within its room allocates nothing`() {
    MeshData().use { mesh ->
      AllocationBudget.assertNoAllocation {
        mesh.clear()
        val a = mesh.vertex(0f, 0f, 0f, 0f, 0f, 1f, 0f, 0f)
        val b = mesh.vertex(1f, 0f, 0f, 0f, 0f, 1f, 1f, 0f)
        val c = mesh.vertex(0f, 1f, 0f, 0f, 0f, 1f, 0f, 1f)
        mesh.triangle(a, b, c)
        mesh.commit()
      }
    }
  }
}

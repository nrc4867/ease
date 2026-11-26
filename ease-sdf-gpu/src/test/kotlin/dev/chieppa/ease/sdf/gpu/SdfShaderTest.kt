package dev.chieppa.ease.sdf.gpu

import dev.chieppa.ease.sdf.field.DistanceField
import dev.chieppa.ease.sdf.shape.Box
import dev.chieppa.ease.sdf.shape.Sphere
import dev.chieppa.ease.sdf.shape.moved
import dev.chieppa.ease.sdf.shape.union
import java.lang.foreign.Arena
import java.lang.foreign.ValueLayout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** A field's WGSL has a line per instruction, and its nodes are written in SdfNode's layout. */
class SdfShaderTest {
  private val shader =
    SdfShader(DistanceField(Sphere(1f).union(Box(1f, 1f, 1f).moved(2f, 0f, 0f), 0.5f)))

  @Test
  fun `the WGSL reads parameters by node, one line per instruction`() {
    val wgsl = shader.wgsl

    assertTrue("fn sdf_field(p0: vec3f) -> f32" in wgsl)
    assertTrue("let d0 = sdf_sphere(p0, sdf_nodes[0u].values);" in wgsl)
    assertTrue("let p1 = sdf_transform(p0, sdf_nodes[1u]);" in wgsl)
    assertTrue("sdf_union(d0, " in wgsl)
  }

  @Test
  fun `nodes are written as their parameters, sixteen floats each`() {
    Arena.ofConfined().use { arena ->
      val nodes = arena.allocate(shader.nodeBytes)
      shader.writeNodes(nodes)

      assertEquals(4, shader.nodeCount)
      assertEquals(1f, nodes.getAtIndex(ValueLayout.JAVA_FLOAT, SPHERE_RADIUS))
      assertEquals(-2f, nodes.getAtIndex(ValueLayout.JAVA_FLOAT, MOVE_X_OFFSET))
      assertEquals(0.5f, nodes.getAtIndex(ValueLayout.JAVA_FLOAT, UNION_BLEND))
    }
  }

  private companion object {
    /** Node 0's first value; node 1's first row's offset; node 3's first value. */
    const val SPHERE_RADIUS = 12L
    const val MOVE_X_OFFSET = 16L + 3L
    const val UNION_BLEND = 48L + 12L
  }
}

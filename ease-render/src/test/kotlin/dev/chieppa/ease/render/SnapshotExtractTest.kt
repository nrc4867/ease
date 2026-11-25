package dev.chieppa.ease.render

import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.ecs.phasedWorld
import dev.chieppa.ease.render.scene.Mesh
import dev.chieppa.ease.render.scene.MeshShape
import kotlin.test.Test
import kotlin.test.assertEquals

/** A world nobody ticks, extracted on demand. */
class SnapshotExtractTest {
  @Test
  fun `an extract outside a tick writes every mesh where it is`() {
    val world = phasedWorld {}
    world.entity { it += LocalTransform() }
    val sphere = world.entity {
      it +=
        LocalTransform().apply {
          x = 3f
          y = -1f
          z = 2f
        }
      it += Mesh.of(MeshShape.SPHERE)
    }
    val snapshot = RenderSnapshot()
    val transform = FloatArray(TRANSFORM_FLOATS)

    snapshot.extractFrom(world)
    snapshot.transformInto(sphere.id, 1f, transform, 0)

    assertEquals(1, snapshot.count)
    assertEquals(sphere.id, snapshot.entityAt(0))
    assertEquals(listOf(3f, -1f, 2f), transform.take(3))
    assertEquals(MeshShape.SPHERE, snapshot.shape(sphere.id))
  }
}

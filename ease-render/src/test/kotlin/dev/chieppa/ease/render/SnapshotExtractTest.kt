package dev.chieppa.ease.render

import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.ecs.phasedWorld
import kotlin.test.Test
import kotlin.test.assertEquals

/** A world nobody ticks, extracted on demand. */
class SnapshotExtractTest {
  @Test
  fun `an extract outside a tick writes every quad where it is`() {
    val world = phasedWorld {}
    world.entity { it += LocalTransform() }
    val quad = world.entity {
      it +=
        LocalTransform().apply {
          x = 3f
          y = -1f
        }
      it += Quad.of(size = 2f, red = 1f, green = 0f, blue = 0f)
    }
    val snapshot = RenderSnapshot()

    snapshot.extractFrom(world)

    assertEquals(1, snapshot.count)
    assertEquals(quad.id, snapshot.entityAt(0))
    assertEquals(3f, snapshot.x(quad.id, alpha = 1f))
    assertEquals(-1f, snapshot.y(quad.id, alpha = 1f))
    assertEquals(2f, snapshot.size[quad.id])
  }
}

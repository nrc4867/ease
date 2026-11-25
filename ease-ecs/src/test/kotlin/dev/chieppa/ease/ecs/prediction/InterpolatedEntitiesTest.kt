package dev.chieppa.ease.ecs.prediction

import com.github.quillraven.fleks.Entity
import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.ecs.LocalTransformReplicated
import dev.chieppa.ease.ecs.phasedWorld
import dev.chieppa.ease.ecs.replication.Authority
import dev.chieppa.ease.ecs.replication.PeerId
import dev.chieppa.ease.ecs.replication.ReplicationSettings
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * A replica a client neither holds nor predicts, drawn between the states its snapshots brought: a
 * rest held over many snapshots stays a rest until the move that ends it.
 */
class InterpolatedEntitiesTest {
  private val world =
    phasedWorld(
      replication = ReplicationSettings(PeerId(1), listOf(LocalTransformReplicated), false)
    ) {}
  private val replica: Entity = world.entity {
    it += LocalTransform()
    it += Authority(PeerId.HOST, PeerId.HOST)
  }
  private val interpolated = InterpolatedEntities(world)

  private fun receive(tick: Long, x: Float) {
    interpolated.beforeReceive()
    with(world) { replica[LocalTransform].x = x }
    interpolated.snapshotApplied(tick)
  }

  private fun drawnAt(time: Double): Float {
    interpolated.present(time)
    return with(world) { replica[LocalTransform].x }
  }

  @Test
  fun `a replica resting through many snapshots starts to move only when it moved`() {
    for (tick in 0L..REST_TICKS) receive(tick, 0f)
    receive(REST_TICKS + 1, 1f)

    assertEquals(0f, drawnAt(REST_TICKS / 2.0))
    assertEquals(0f, drawnAt(REST_TICKS.toDouble()))
    assertEquals(0.5f, drawnAt(REST_TICKS + 0.5), TOLERANCE)
    assertEquals(1f, drawnAt(REST_TICKS + 2.0))
  }

  @Test
  fun `between two received states the replica is blended, and before the first it holds`() {
    receive(10, 0f)
    receive(14, 4f)

    assertEquals(1f, drawnAt(11.0), TOLERANCE)
    assertEquals(0f, drawnAt(5.0))
  }

  private companion object {
    const val REST_TICKS = 10L
    const val TOLERANCE = 1e-5f
  }
}

package dev.chieppa.ease.ecs.replication

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World
import com.github.quillraven.fleks.World.Companion.family
import dev.chieppa.ease.ecs.Phase
import dev.chieppa.ease.ecs.phasedWorld
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * A client's own entities, held by the host: only its predicted systems may write them, nothing
 * else may, and predicted systems still may not write anything it neither holds nor owns.
 */
class PredictedWriteTest {
  private val client = PeerId(1)

  /** Fuels every lantern it may write, or, when [everyLantern], every lantern there is. */
  private class Fuel(private val everyLantern: Boolean = false) :
    IteratingSystem(family { all(Lantern) }) {
    private val replication = world.replication

    override fun onTickEntity(entity: Entity) {
      if (everyLantern || replication.mayWrite(entity)) {
        replication.write(entity, Lantern) { fuel += 1f }
      }
    }
  }

  private class Plain(private val everyLantern: Boolean) :
    IteratingSystem(family { all(Lantern) }) {
    private val replication = world.replication

    override fun onTickEntity(entity: Entity) {
      if (everyLantern) replication.write(entity, Lantern) { charges += 1 }
    }
  }

  private fun world(predictedWritesAll: Boolean = false, plainWrites: Boolean = false): World =
    phasedWorld(replication = ReplicationSettings(client, ReplicationWorlds.serializers, true)) {
      add(Phase.SIMULATION, Plain(plainWrites))
      predicted { add(Phase.SIMULATION, Fuel(predictedWritesAll)) }
    }

  private fun lantern(world: World, owner: PeerId): Entity = world.entity {
    it += Lantern()
    it += Authority(PeerId.HOST, owner)
  }

  @Test
  fun `predicted systems write the entities the world predicts, and only those`() {
    val world = world()
    val own = lantern(world, client)
    val other = lantern(world, PeerId(2))

    world.update(DELTA)

    assertTrue(world.replication.predicts(own))
    assertFalse(world.replication.predicts(other))
    assertEquals(1f, ReplicationWorlds.lanternOf(world, own).fuel)
    assertEquals(0f, ReplicationWorlds.lanternOf(world, other).fuel)
    assertFalse(world.replication.mayWrite(own), "outside the predicted systems")
  }

  @Test
  fun `a predicted system writing another peer's entity throws in a debug build`() {
    val world = world(predictedWritesAll = true)
    lantern(world, PeerId(2))

    assertFailsWith<AuthorityViolation> { world.update(DELTA) }
  }

  @Test
  fun `any other system writing a predicted entity throws in a debug build, naming why`() {
    val world = world(plainWrites = true)
    lantern(world, client)

    val thrown = assertFailsWith<AuthorityViolation> { world.update(DELTA) }

    assertTrue("outside a predicted system" in thrown.message.orEmpty(), thrown.message)
  }

  @Test
  fun `a predicted step runs the predicted systems alone`() {
    val world = world()
    val own = lantern(world, client)
    world.update(DELTA)

    world.replication.stepPredicted()
    world.replication.stepPredicted()

    assertEquals(3f, ReplicationWorlds.lanternOf(world, own).fuel)
    assertFalse(world.replication.predicting)
  }

  @Test
  fun `predicted systems of one phase must follow one another`() {
    assertFailsWith<IllegalStateException> {
      phasedWorld {
        predicted { add(Phase.INPUT, Fuel()) }
        add(Phase.INPUT, Plain(false))
        predicted { add(Phase.INPUT, Other()) }
      }
    }
  }

  private class Other : IteratingSystem(family { all(Lantern) }) {
    override fun onTickEntity(entity: Entity) = Unit
  }

  private companion object {
    const val DELTA = 1f / 60f
  }
}

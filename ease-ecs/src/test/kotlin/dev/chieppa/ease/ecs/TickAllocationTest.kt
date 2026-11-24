package dev.chieppa.ease.ecs

import dev.chieppa.ease.testing.AllocationBudget
import kotlin.test.Test

class TickAllocationTest {
  @Test
  fun `a tick that moves ten thousand entities allocates nothing`() {
    val world = phasedWorld(ENTITIES) { add(Phase.SIMULATION, MovementSystem()) }
    repeat(ENTITIES) { _ ->
      world.entity {
        it += Position()
        it += Velocity(1f, 1f)
      }
    }

    AllocationBudget.assertWithin(0, { world.update(DELTA) }, WARMUP_TICKS, MEASURED_TICKS)
  }

  private companion object {
    const val ENTITIES = 10_000
    const val DELTA = 1f / 60f
    const val WARMUP_TICKS = 50
    const val MEASURED_TICKS = 100
  }
}

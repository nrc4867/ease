package dev.chieppa.ease.ecs

import kotlin.test.Test
import kotlin.test.assertEquals

class PhasedWorldTest {
  @Test
  fun `systems registered out of order run in phase order`() {
    val log = mutableListOf<String>()
    val world = phasedWorld {
      add(Phase.EXTRACT, RecordsA(log))
      add(Phase.INPUT, RecordsB(log))
      add(Phase.ANIMATION, RecordsC(log))
      add(Phase.SIMULATION, RecordsD(log))
      add(Phase.PHYSICS, RecordsE(log))
    }

    world.update(1f)

    assertEquals(listOf("b", "d", "e", "c", "a"), log)
  }

  @Test
  fun `systems in one phase run in registration order`() {
    val log = mutableListOf<String>()
    val world = phasedWorld {
      add(Phase.SIMULATION, RecordsB(log))
      add(Phase.INPUT, RecordsC(log))
      add(Phase.SIMULATION, RecordsA(log))
    }

    world.update(1f)

    assertEquals(listOf("c", "b", "a"), log)
  }

  @Test
  fun `each update runs every phase once`() {
    val log = mutableListOf<String>()
    val world = phasedWorld {
      add(Phase.PHYSICS, RecordsA(log))
      add(Phase.INPUT, RecordsB(log))
    }

    world.update(1f)
    world.update(1f)

    assertEquals(listOf("b", "a", "b", "a"), log)
  }
}

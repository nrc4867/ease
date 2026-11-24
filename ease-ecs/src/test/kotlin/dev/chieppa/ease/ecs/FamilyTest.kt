package dev.chieppa.ease.ecs

import com.github.quillraven.fleks.World
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FamilyTest {
  private fun movingWorld(): World = phasedWorld { add(Phase.SIMULATION, MovementSystem()) }

  @Test
  fun `a family iterates only matching entities`() {
    val world = movingWorld()
    val mover = world.entity {
      it += Position()
      it += Velocity(1f, 2f)
    }
    val still = world.entity { it += Position() }

    world.update(1f)

    with(world) {
      assertEquals(1f, mover[Position].x)
      assertEquals(2f, mover[Position].y)
      assertEquals(0f, still[Position].x)
    }
  }

  @Test
  fun `adding and removing a component changes family membership`() {
    val world = movingWorld()
    val family = world.family { all(Position, Velocity) }
    val entity = world.entity { it += Position() }
    assertFalse(entity in family)

    with(world) { entity.configure { it += Velocity(1f, 0f) } }
    assertTrue(entity in family)
    world.update(1f)
    assertEquals(1f, with(world) { entity[Position].x })

    with(world) { entity.configure { it -= Velocity } }
    assertFalse(entity in family)
    world.update(1f)
    assertEquals(1f, with(world) { entity[Position].x })
  }
}

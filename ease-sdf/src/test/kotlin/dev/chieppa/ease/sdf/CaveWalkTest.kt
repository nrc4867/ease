package dev.chieppa.ease.sdf

import com.github.quillraven.fleks.Entity
import dev.chieppa.ease.sdf.mesh.SurfaceNets
import dev.chieppa.ease.testing.AllocationBudget
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * A character walks through the cave built from code: along the tunnel's floor, around the bend
 * through the chamber to the far opening, stopped by the walls on the way.
 */
class CaveWalkTest {
  private val cave = CaveWorld(SurfaceNets.mesh(TestCave.field(), TestCave.grid()))

  @AfterTest fun close() = cave.close()

  @Test
  fun `a character walks the tunnel on its floor, around the bend to the far opening`() {
    val walker = cave.character(-12f, TestCave.FLOOR + 0.1f, 0f)

    walkTo(walker, x = SPEED, z = 0f, ticks = SECONDS_5) { it.x >= 4f }
    assertOnFloor(walker)
    walkTo(walker, x = 0f, z = -SPEED, ticks = SECONDS_5) { it.z <= -9f }

    val end = cave.transformOf(walker)
    assertTrue(end.z <= -9f, "reached the far opening, z ${end.z}")
    assertTrue(end.x in 2.5f..5.5f, "stayed in the passage, x ${end.x}")
  }

  @Test
  fun `the tunnel's walls stop a character walking into them`() {
    val walker = cave.character(-8f, TestCave.FLOOR + 0.1f, 0f)
    cave.step(SETTLE)

    cave.walk(walker, x = 0f, z = SPEED)
    cave.step(SECONDS_2)

    val stopped = cave.transformOf(walker)
    assertTrue(stopped.z < TestCave.TUNNEL_RADIUS, "held inside the tunnel, z ${stopped.z}")
    assertTrue(stopped.z > 0.5f, "walked up to the wall, z ${stopped.z}")
    assertEquals(-8f, stopped.x, 0.1f)
  }

  @Test
  fun `a tick in the cave allocates nothing`() {
    val walker = cave.character(-12f, TestCave.FLOOR + 0.1f, 0f)
    var tick = 0

    AllocationBudget.assertWithin(
      0,
      {
        val forward = if (tick++ / TURN_TICKS % 2 == 0) SPEED else -SPEED
        cave.walk(walker, x = forward, z = 0f)
        cave.step()
      },
      WARMUP_TICKS,
      MEASURED_TICKS,
    )
  }

  private fun walkTo(
    walker: Entity,
    x: Float,
    z: Float,
    ticks: Int,
    arrived: (dev.chieppa.ease.ecs.LocalTransform) -> Boolean,
  ) {
    cave.walk(walker, x, z)
    var left = ticks
    while (left-- > 0 && !arrived(cave.transformOf(walker))) {
      cave.step()
      assertOnFloor(walker)
    }
  }

  private fun assertOnFloor(walker: Entity) {
    val y = cave.transformOf(walker).y
    assertTrue(y in TestCave.FLOOR - 0.15f..TestCave.FLOOR + 0.6f, "on the floor, y $y")
  }

  private companion object {
    const val SPEED = 3f
    const val SETTLE = 30
    const val SECONDS_2 = 120
    const val SECONDS_5 = 300
    const val TURN_TICKS = 60
    const val WARMUP_TICKS = 1_000
    const val MEASURED_TICKS = 300
  }
}

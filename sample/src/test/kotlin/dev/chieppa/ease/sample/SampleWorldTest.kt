package dev.chieppa.ease.sample

import dev.chieppa.ease.commands.CommandOrigin
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** The sample world run headless: ticks advanced by hand, no window and no GPU. */
class SampleWorldTest {
  private val sample = SampleWorld()

  @Test
  fun `the player's loader finds the sample game, which spawns its squares through commands`() {
    assertIs<SampleGame>(sample.module)
    assertEquals(SampleGame.SQUARES.size, sample.entities.size)
    val logged = sample.hosted.history.log
    assertTrue(logged.all.all { it.origin == CommandOrigin.GAME })
    assertEquals(SampleGame.SQUARES.size, logged.all.size)
  }

  @Test
  fun `before reaching a wall each square moves by its speed every tick`() {
    sample.step(SHORT_RUN)

    val seconds = sample.stepSeconds * SHORT_RUN
    sample.entities.zip(SampleGame.SQUARES).forEach { (entity, square) ->
      assertEquals(square.x + square.speedX * seconds, sample.x(entity), TOLERANCE)
      assertEquals(square.y + square.speedY * seconds, sample.y(entity), TOLERANCE)
    }
  }

  @Test
  fun `squares bounce and stay in the playfield`() {
    sample.step(LONG_RUN)

    val margin = MAX_SPEED * sample.stepSeconds
    sample.entities.forEach { entity ->
      assertTrue(abs(sample.x(entity)) <= Playfield.HALF_WIDTH + margin, "x ${sample.x(entity)}")
      assertTrue(abs(sample.y(entity)) <= Playfield.HALF_HEIGHT + margin, "y ${sample.y(entity)}")
    }
  }

  @Test
  fun `the extract snapshot holds the last two ticks of every square`() {
    sample.step(SHORT_RUN - 1)
    val before = sample.entities.map { sample.x(it) to sample.y(it) }
    sample.step(1)

    val snapshot = sample.hosted.snapshot
    assertEquals(sample.entities.size, snapshot.count)
    sample.entities.zip(before).forEach { (entity, previous) ->
      assertEquals(previous.first, snapshot.previousX[entity.id])
      assertEquals(previous.second, snapshot.previousY[entity.id])
      assertEquals(sample.x(entity), snapshot.currentX[entity.id])
      assertEquals(sample.y(entity), snapshot.currentY[entity.id])
    }
  }

  private companion object {
    const val SHORT_RUN = 30
    const val LONG_RUN = 2_000
    const val TOLERANCE = 1e-3f
    const val MAX_SPEED = 4f
  }
}

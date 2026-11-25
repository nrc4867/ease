package dev.chieppa.ease.sample

import dev.chieppa.ease.commands.CommandOrigin
import dev.chieppa.ease.scene.commands.SpawnNode
import java.nio.file.Path
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import org.junit.jupiter.api.io.TempDir

/** The sample world run headless: ticks advanced by hand, no window and no GPU. */
class SampleWorldTest {
  @TempDir lateinit var folder: Path

  private val sample by lazy { SampleWorld(folder) }

  @Test
  fun `the player's loader finds the sample game, which spawns its scene's squares as nodes`() {
    assertIs<SampleGame>(sample.module)
    val nodes = sample.content.squares().size
    assertEquals(nodes, sample.entities.size)
    val log = sample.hosted.history.log
    assertTrue(log.all.all { it.origin == CommandOrigin.GAME && it.tick == 0L })
    assertEquals(List(nodes) { SpawnNode::class }, log.all.map { log.commandOf(it)::class })
  }

  @Test
  fun `before reaching a wall each square moves by its speed every tick`() {
    val start = sample.entities.map { Triple(sample.x(it), sample.y(it), sample.velocity(it)) }
    sample.step(SHORT_RUN)

    val seconds = sample.stepSeconds * SHORT_RUN
    sample.entities.zip(start).forEach { (entity, at) ->
      val (x, y, speed) = at
      assertEquals(x + speed.first * seconds, sample.x(entity), TOLERANCE)
      assertEquals(y + speed.second * seconds, sample.y(entity), TOLERANCE)
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

package dev.chieppa.ease.physics

import com.github.quillraven.fleks.Entity
import dev.chieppa.ease.ecs.replication.PeerId
import dev.chieppa.ease.ecs.replication.replication
import dev.chieppa.ease.ecs.replication.stepPredicted
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals

/**
 * A character walking a course of slopes, steps and walls from recorded input replays to the same
 * bits: in a second world from the start, and in the same world rewound to an earlier tick, as a
 * client reconciling its own character does.
 */
class ReplayTest {
  private val client = PeerId(1)
  private val worlds = mutableListOf<TestWorld>()

  @AfterTest fun close() = worlds.forEach { it.close() }

  private fun course(): Pair<TestWorld, Entity> {
    // A client world predicting its own character, held by the host.
    val test = TestWorld(peer = client, debugChecks = false).also { worlds += it }
    test.floor()
    test.box(4f, 0f, 0f, 3f, 0.1f, 2f, degreesAboutZ = 25f)
    test.box(0f, 0.15f, 4f, 2f, 0.15f, 1f)
    test.box(-4f, 1f, 0f, 0.25f, 1f, 6f)
    return test to test.character(0f, owner = client)
  }

  /** Recorded input: a walk that turns every half second, with a jump now and then. */
  private fun input(tick: Int): FloatArray {
    val turn = tick / TURN_TICKS
    val angle = turn * TURN_RADIANS
    val jump = if (tick % JUMP_TICKS == 0) JUMP_SPEED else 0f
    return floatArrayOf(kotlin.math.cos(angle) * SPEED, jump, kotlin.math.sin(angle) * SPEED)
  }

  private fun apply(test: TestWorld, character: Entity, tick: Int) {
    val (x, y, z) = input(tick)
    test.walk(character, x = x, y = y, z = z)
  }

  private fun state(test: TestWorld, character: Entity): IntArray {
    val t = test.transformOf(character)
    val m = test.motionOf(character)
    return intArrayOf(
      t.x.toRawBits(),
      t.y.toRawBits(),
      t.z.toRawBits(),
      m.velocityX.toRawBits(),
      m.velocityY.toRawBits(),
      m.velocityZ.toRawBits(),
      m.stride.toRawBits(),
      m.groundSurface,
    )
  }

  private fun restore(test: TestWorld, character: Entity, state: IntArray) {
    val t = test.transformOf(character)
    val m = test.motionOf(character)
    t.x = Float.fromBits(state[0])
    t.y = Float.fromBits(state[1])
    t.z = Float.fromBits(state[2])
    m.velocityX = Float.fromBits(state[3])
    m.velocityY = Float.fromBits(state[4])
    m.velocityZ = Float.fromBits(state[5])
    m.stride = Float.fromBits(state[6])
    m.groundSurface = state[7]
  }

  private fun record(test: TestWorld, character: Entity): List<IntArray> =
    (0 until TICKS).map { tick ->
      apply(test, character, tick)
      test.step()
      state(test, character)
    }

  @Test
  fun `a second world replaying the recorded input matches tick for tick`() {
    val (first, firstCharacter) = course()
    val (second, secondCharacter) = course()

    val recorded = record(first, firstCharacter)
    val replayed = record(second, secondCharacter)

    for (tick in 0 until TICKS) assertContentEquals(recorded[tick], replayed[tick], "tick $tick")
  }

  @Test
  fun `rewound to an earlier tick and replayed, the character retraces its path exactly`() {
    val (test, character) = course()
    val recorded = record(test, character)

    for (from in REWIND_TICKS) {
      restore(test, character, recorded[from])
      for (tick in from + 1 until TICKS) {
        apply(test, character, tick)
        test.world.replication.stepPredicted()
        assertContentEquals(recorded[tick], state(test, character), "from $from, tick $tick")
      }
    }
  }

  private companion object {
    const val TICKS = 600
    const val TURN_TICKS = 30
    const val TURN_RADIANS = 1.1f
    const val JUMP_TICKS = 97
    const val JUMP_SPEED = 4f
    const val SPEED = 3f
    val REWIND_TICKS = listOf(10, 250, 420)
  }
}

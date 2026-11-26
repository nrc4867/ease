package dev.chieppa.ease.animation

import dev.chieppa.ease.animation.TestRig.IDLE
import dev.chieppa.ease.animation.TestRig.WALK
import dev.chieppa.ease.animation.TestRig.WALK_SPEED
import kotlin.math.abs
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The rig idles at speed 0, walks at walking speed and blends the two in between, crossfading
 * between its idle and move states when speed crosses the threshold.
 */
class BlendBySpeedTest {
  private val test = AnimatedWorld()
  private val rig = TestRig.place(test.world)

  @AfterTest fun close() = test.close()

  private fun runAt(speed: Float, ticks: Int = SETTLE_TICKS) {
    repeat(ticks) {
      test.animators.setParameter(rig.animator, "speed", speed)
      test.step()
    }
  }

  @Test
  fun `at speed 0 the rig idles`() {
    runAt(0f)

    assertEquals("idle", test.animators.stateOf(rig.animator))
    assertEquals(1f, test.animators.clipWeight(rig.animator, IDLE))
    assertEquals(0f, test.animators.clipWeight(rig.animator, WALK))
  }

  @Test
  fun `at walking speed the rig walks`() {
    runAt(WALK_SPEED)

    assertEquals("move", test.animators.stateOf(rig.animator))
    assertEquals(1f, test.animators.clipWeight(rig.animator, WALK))
    assertEquals(0f, test.animators.clipWeight(rig.animator, IDLE))
  }

  @Test
  fun `between the two the weights follow the speed`() {
    for (share in listOf(0.25f, 0.5f, 0.75f)) {
      runAt(WALK_SPEED * share)

      assertEquals("move", test.animators.stateOf(rig.animator))
      assertEquals(share, test.animators.clipWeight(rig.animator, WALK), TOLERANCE)
      assertEquals(1f - share, test.animators.clipWeight(rig.animator, IDLE), TOLERANCE)
    }
  }

  @Test
  fun `leaving idle crossfades over the transition's duration`() {
    runAt(0f)
    runAt(WALK_SPEED, ticks = HALF_FADE_TICKS)

    assertEquals("move", test.animators.stateOf(rig.animator))
    assertEquals(0.5f, test.animators.clipWeight(rig.animator, WALK), FADE_TOLERANCE)

    runAt(WALK_SPEED, ticks = HALF_FADE_TICKS + 1)
    assertEquals(1f, test.animators.clipWeight(rig.animator, WALK))
  }

  @Test
  fun `the walk swings the legs, so the feet leave their rest places`() {
    runAt(0f)
    val resting = test.worldOf(rig.footLeft)[2]
    runAt(WALK_SPEED, ticks = QUARTER_STRIDE_TICKS)

    val walking = test.worldOf(rig.footLeft)[2]
    assertEquals(0f, resting, TOLERANCE)
    assertTrue(abs(walking - resting) > MIN_SWING, "the foot stayed at z = $walking")
  }

  private companion object {
    const val SETTLE_TICKS = 40
    const val HALF_FADE_TICKS = 8
    const val QUARTER_STRIDE_TICKS = 15
    const val TOLERANCE = 1e-5f
    const val FADE_TOLERANCE = 0.07f
    const val MIN_SWING = 0.1f
  }
}

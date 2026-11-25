package dev.chieppa.ease.physics

import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.ecs.LocalTransformReplicated
import dev.chieppa.ease.ecs.replication.replication
import dev.chieppa.ease.ecs.replication.stepPredicted
import dev.chieppa.ease.physics.body.MotionKind
import dev.chieppa.ease.physics.body.RigidBody
import dev.chieppa.ease.physics.character.CharacterController
import dev.chieppa.ease.physics.character.CharacterMotion
import dev.chieppa.ease.physics.character.CharacterMotionReplicated
import kotlin.math.abs
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Footsteps count only the walking asked for: one per stride however many fit in a tick, none while
 * carried by a moving platform, and none again when a tick is replayed after a correction.
 */
class StrideTest {
  private val test = TestWorld()
  private val events = RecordingListener().also { test.physics.events.add(it) }

  @AfterTest fun close() = test.close()

  @Test
  fun `several strides in one tick report a footstep each`() {
    test.floor()
    val character = test.character(0f)
    with(test.world) { character[CharacterController] }.strideLength = SHORT_STRIDE

    test.walk(character, x = SPEED)
    test.step(TICKS)

    val asked = SPEED * TICKS * TestWorld.STEP
    val expected = (asked / SHORT_STRIDE).toInt()
    assertTrue(expected > TICKS, "more strides than ticks")
    assertTrue(abs(events.footsteps.size - expected) <= 1, "${events.footsteps.size} of $expected")
  }

  @Test
  fun `standing on a moving platform is carried without a footstep`() {
    val platform = test.box(0f, -HALF_THICK, 0f, PLATFORM, HALF_THICK, PLATFORM)
    with(test.world) {
      platform.configure { it += RigidBody().apply { motion = MotionKind.KINEMATIC } }
    }
    val character = test.character(0f)
    test.step(SETTLE)
    events.footsteps.clear()

    repeat(TICKS) {
      test.world.replication.write(platform, LocalTransform) { x += SPEED * TestWorld.STEP }
      test.step()
    }

    assertTrue(test.transformOf(character).x > SPEED * TICKS * TestWorld.STEP / 2, "carried")
    assertEquals(emptyList(), events.footsteps)
  }

  @Test
  fun `a correction five ticks back replays without reporting footsteps again`() {
    test.floor()
    val character = test.character(0f)
    test.walk(character, x = SPEED)
    test.step(TICKS - REWIND)
    val transform =
      LocalTransform().also { LocalTransformReplicated.copy(test.transformOf(character), it) }
    val motion =
      CharacterMotion().also { CharacterMotionReplicated.copy(test.motionOf(character), it) }
    test.step(REWIND)
    val footsteps = events.footsteps.size
    val reached = test.transformOf(character).x

    LocalTransformReplicated.copy(transform, test.transformOf(character))
    CharacterMotionReplicated.copy(motion, test.motionOf(character))
    repeat(REWIND) { test.world.replication.stepPredicted() }

    assertEquals(reached, test.transformOf(character).x, "replayed to the same place")
    assertEquals(footsteps, events.footsteps.size, "no footstep reported twice")
    assertEquals(listOf(0), events.groundChanges)
  }

  private companion object {
    const val SHORT_STRIDE = 0.01f
    const val SPEED = 3f
    const val TICKS = 60
    const val SETTLE = 10
    const val REWIND = 5
    const val PLATFORM = 3f
    const val HALF_THICK = 0.25f
  }
}

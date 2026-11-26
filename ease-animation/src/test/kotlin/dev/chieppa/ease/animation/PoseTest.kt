package dev.chieppa.ease.animation

import com.github.quillraven.fleks.Entity
import dev.chieppa.ease.animation.TestRig.WALK_SPEED
import dev.chieppa.ease.animation.assets.ParameterDrive
import dev.chieppa.ease.animation.scene.SocketFollower
import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.ecs.replication.replication
import dev.chieppa.ease.scene.node.LocalId
import dev.chieppa.ease.scene.node.NodeKey
import dev.chieppa.ease.scene.slots.NodeRef
import dev.chieppa.ease.testing.AllocationBudget
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

/**
 * The pose is derived: joints keep their own transforms while their world transforms follow the
 * clips, nodes follow sockets, two worlds given the same parameters pose identically, motion drives
 * speed, and a steady tick allocates nothing.
 */
class PoseTest {
  private val test = AnimatedWorld()
  private val rig = TestRig.place(test.world)

  @AfterTest fun close() = test.close()

  private fun walk(world: AnimatedWorld, animator: Entity, ticks: Int) {
    repeat(ticks) {
      world.animators.setParameter(animator, "speed", WALK_SPEED * (it % SPEED_STEPS) / SPEED_STEPS)
      world.step()
    }
  }

  @Test
  fun `joints keep their own transforms while the pose moves them`() {
    walk(test, rig.animator, TICKS)

    val hips = with(test.world) { rig.hips[LocalTransform] }
    assertEquals(1f, hips.y)
    assertEquals(0f, hips.x)
  }

  @Test
  fun `a socket follower sits at its socket, offset in the bone's space`() {
    val follower =
      test.world.entity {
        it += LocalTransform().apply { y = 0.1f }
        it += SocketFollower(NodeRef(NodeKey.of(LocalId(1))), "hand")
      }
    walk(test, rig.animator, TICKS)

    val hips = test.worldOf(rig.hips)
    val placed = test.worldOf(follower)
    assertEquals(hips[0] + 0.5f, placed[0], TOLERANCE)
    assertEquals(hips[1] + 0.1f, placed[1], TOLERANCE)
    assertEquals(hips[2], placed[2], TOLERANCE)
  }

  @Test
  fun `two worlds given the same parameters pose their rigs to the bit`() {
    AnimatedWorld().use { other ->
      val twin = TestRig.place(other.world)
      walk(test, rig.animator, TICKS)
      walk(other, twin.animator, TICKS)

      assertContentEquals(test.worldOf(rig.footLeft), other.worldOf(twin.footLeft))
      assertContentEquals(test.worldOf(rig.footRight), other.worldOf(twin.footRight))
    }
  }

  @Test
  fun `a motion-driven speed follows how fast the node moves across the ground`() {
    AnimatedWorld(ParameterDrive.MOTION_SPEED).use { moving ->
      val placed = TestRig.place(moving.world)
      repeat(TICKS) {
        moving.world.replication.write(placed.animator, LocalTransform) {
          z += WALK_SPEED * AnimatedWorld.STEP
        }
        moving.step()
      }

      assertEquals(
        WALK_SPEED,
        moving.animators.parameter(placed.animator, "speed"),
        SPEED_TOLERANCE,
      )
      assertEquals("move", moving.animators.stateOf(placed.animator))
    }
  }

  @Test
  fun `a steady tick allocates nothing`() {
    var tick = 0
    AllocationBudget.assertWithin(
      0,
      {
        val speed = WALK_SPEED * (tick++ % SPEED_STEPS) / SPEED_STEPS
        test.animators.setParameter(rig.animator, "speed", speed)
        test.step()
      },
      WARMUP_TICKS,
      MEASURED_TICKS,
    )
  }

  private companion object {
    const val TICKS = 100
    const val SPEED_STEPS = 7
    const val TOLERANCE = 1e-5f
    const val SPEED_TOLERANCE = 1e-3f
    const val WARMUP_TICKS = 2_000
    const val MEASURED_TICKS = 500
  }
}

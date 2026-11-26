package dev.chieppa.ease.animation

import com.github.quillraven.fleks.Entity
import dev.chieppa.ease.animation.TestRig.LEFT_DOWN
import dev.chieppa.ease.animation.TestRig.RIGHT_DOWN
import dev.chieppa.ease.animation.TestRig.WALK_SPEED
import dev.chieppa.ease.animation.events.AnimationListener
import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.physics.body.Collider
import dev.chieppa.ease.physics.setup.PhysicsSettings
import dev.chieppa.ease.physics.setup.SurfaceType
import dev.chieppa.ease.physics.setup.SurfaceTypes
import dev.chieppa.ease.testing.AllocationBudget
import kotlin.math.ceil
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Footstep events fire at the walk clip's event times, and each foot's probe, run on its own
 * footstep, reports the surface under that foot: stone under the left, grass under the right.
 */
class FootstepProbeTest {
  private val surfaces = SurfaceTypes.of(SurfaceType("stone"), SurfaceType("grass"))
  private val test = AnimatedWorld(physics = PhysicsSettings(surfaces = surfaces))
  private val rig = TestRig.place(test.world)
  private val heard = mutableListOf<String>()
  private var tick = 0

  init {
    ground(-HALF_WIDTH, "stone")
    ground(HALF_WIDTH, "grass")
  }

  /** Writes down every event and probe report, with the tick it came in. */
  private val recorder =
    object : AnimationListener {
      override fun clipEvent(animator: Entity, event: String) {
        heard += "$tick event $event"
      }

      override fun probe(
        animator: Entity,
        probe: String,
        surface: Int,
        x: Float,
        y: Float,
        z: Float,
      ) {
        heard += "$tick probe $probe ${surfaces[surface].name}"
      }
    }

  @AfterTest fun close() = test.close()

  /** A static box of ground, its top at y = 0, centered at x = [x], one unit wide. */
  private fun ground(x: Float, surface: String) {
    test.world.entity {
      it +=
        LocalTransform().apply {
          this.x = x
          y = -HALF_HEIGHT
        }
      it +=
        Collider().apply {
          halfX = HALF_WIDTH
          halfY = HALF_HEIGHT
          halfZ = HALF_DEPTH
          this.surface = surface
        }
    }
  }

  private fun walk(ticks: Int) {
    repeat(ticks) {
      test.animators.setParameter(rig.animator, "speed", WALK_SPEED)
      tick++
      test.step()
    }
  }

  @Test
  fun `each foot's probe runs on its footstep and finds the ground under that foot`() {
    test.animators.events.add(recorder)
    walk(SETTLE_TICKS)
    heard.clear()
    tick = 0

    walk(CYCLE_TICKS)

    val left = ceil(LEFT_DOWN * TICKS_PER_SECOND).toInt()
    val right = ceil(RIGHT_DOWN * TICKS_PER_SECOND).toInt()
    assertEquals(
      listOf(
        "$left event footstepLeft",
        "$left probe leftFoot stone",
        "$right event footstepRight",
        "$right probe rightFoot grass",
      ),
      heard,
    )
  }

  @Test
  fun `walking with probes on the ground allocates nothing`() {
    var probes = 0
    test.animators.events.add(
      object : AnimationListener {
        override fun probe(
          animator: Entity,
          probe: String,
          surface: Int,
          x: Float,
          y: Float,
          z: Float,
        ) {
          probes++
        }
      }
    )

    AllocationBudget.assertWithin(0, { walk(1) }, WARMUP_TICKS, MEASURED_TICKS)
    assertTrue(probes > MEASURED_TICKS / TICKS_PER_SECOND, "only $probes probes ran")
  }

  private companion object {
    const val WARMUP_TICKS = 2_000
    const val MEASURED_TICKS = 500
    const val TICKS_PER_SECOND = 60
    const val SETTLE_TICKS = 60
    const val CYCLE_TICKS = 60
    const val HALF_WIDTH = 0.5f
    const val HALF_HEIGHT = 0.5f
    const val HALF_DEPTH = 5f
  }
}

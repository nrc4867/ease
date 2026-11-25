package dev.chieppa.ease.physics

import com.github.quillraven.fleks.IntervalSystem
import dev.chieppa.ease.ecs.Phase
import dev.chieppa.ease.physics.body.Collider
import dev.chieppa.ease.physics.body.RigidBody
import dev.chieppa.ease.physics.events.PhysicsListener
import dev.chieppa.ease.physics.query.RayHit
import dev.chieppa.ease.physics.volume.TriggerVolume
import dev.chieppa.ease.physics.volume.VolumeSensor
import dev.chieppa.ease.testing.AllocationBudget
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.AfterTest
import kotlin.test.Test

/**
 * A tick with every physics path running allocates nothing: bodies following transforms and dynamic
 * bodies writing theirs, characters walking with footsteps and ground changes, sensors in triggers,
 * and raycasts.
 */
class PhysicsAllocationTest {
  /** Casts a few rays a tick, as gameplay would. */
  private class Rays : IntervalSystem() {
    private val hit = RayHit()
    var hits = 0

    private lateinit var physics: PhysicsWorld

    override fun onInit() {
      physics = world.physics
    }

    override fun onTick() {
      for (index in 0 until RAYS) {
        if (physics.raycast(index.toFloat(), 5f, 0f, 0f, -1f, 0f, 10f, hit)) hits++
        physics.surfaceBelow(index.toFloat(), 1f, 1f, 2f)
      }
    }
  }

  private val test = TestWorld(extra = { it.add(Phase.SIMULATION, Rays()) })

  @AfterTest fun close() = test.close()

  @Test
  fun `a tick with characters, triggers, bodies and rays allocates nothing`() {
    test.physics.events.add(object : PhysicsListener {})
    test.floor()
    test.box(4f, 0f, 0f, 3f, 0.1f, 2f, degreesAboutZ = 25f)
    test.box(-4f, 1f, 0f, 0.25f, 1f, 6f)
    test.world.entity {
      it += TestWorld.placed(0f, 1f, 2f, 0f)
      it +=
        Collider().apply {
          halfX = 2f
          halfZ = 2f
        }
      it += TriggerVolume()
    }
    test.world.entity {
      it += TestWorld.placed(2f, 1f, -3f, 0f)
      it += Collider()
      it += RigidBody()
    }
    val characters =
      Array(CHARACTERS) { index ->
        test.character(index.toFloat() - 2f) { world, entity ->
          with(world) { entity.configure { it += VolumeSensor() } }
        }
      }
    var tick = 0

    AllocationBudget.assertWithin(
      0,
      {
        val angle = (tick++ / TURN_TICKS) * TURN_RADIANS
        for (index in characters.indices) {
          test.walk(characters[index], x = cos(angle) * SPEED, z = sin(angle) * SPEED)
        }
        test.step()
      },
      WARMUP_TICKS,
      MEASURED_TICKS,
    )
  }

  private companion object {
    const val RAYS = 4
    const val CHARACTERS = 4
    const val SPEED = 3f
    const val TURN_TICKS = 40
    const val TURN_RADIANS = 1.3f
    const val WARMUP_TICKS = 2_000
    const val MEASURED_TICKS = 500
  }
}

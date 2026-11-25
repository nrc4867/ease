package dev.chieppa.ease.physics

import dev.chieppa.ease.physics.body.Collider
import dev.chieppa.ease.physics.body.RigidBody
import dev.chieppa.ease.physics.query.RayHit
import dev.chieppa.ease.physics.setup.PhysicsSettings
import dev.chieppa.ease.physics.setup.SurfaceType
import dev.chieppa.ease.physics.setup.SurfaceTypes
import dev.chieppa.ease.physics.volume.TriggerVolume
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Raycasts and surface queries against colliders; dynamic bodies fall and land. */
class QueryTest {
  private val surfaces = SurfaceTypes.of(SurfaceType(SAND))
  private val test = TestWorld(PhysicsSettings(surfaces = surfaces))

  @AfterTest fun close() = test.close()

  @Test
  fun `a raycast reports the entity, distance, point and surface it hits`() {
    val floor = test.floor(surface = SAND)
    val hit = RayHit()

    assertTrue(test.physics.raycast(1f, 3f, 2f, 0f, -2f, 0f, 10f, hit))

    assertEquals(floor, hit.entity)
    assertEquals(3f, hit.distance, TOLERANCE)
    assertEquals(0f, hit.y, TOLERANCE)
    assertEquals(1f, hit.x, TOLERANCE)
    assertEquals(surfaces.indexOf(SAND), hit.surface)
    assertEquals(surfaces.indexOf(SAND), test.physics.surfaceBelow(0f, 1f, 0f, 2f))
  }

  @Test
  fun `a raycast on a layer the game has not named is refused`() {
    test.floor()
    val hit = RayHit()

    assertFailsWith<IllegalArgumentException> {
      test.physics.raycast(0f, 5f, 0f, 0f, -1f, 0f, 6f, hit, layer = 1)
    }
    assertFailsWith<IllegalArgumentException> {
      test.physics.raycast(0f, 5f, 0f, 0f, -1f, 0f, 6f, hit, layer = -2)
    }
    assertTrue(test.physics.raycast(0f, 5f, 0f, 0f, -1f, 0f, 6f, hit, layer = 0))
  }

  @Test
  fun `a raycast misses what is beyond its reach, and never hits a trigger`() {
    test.floor()
    test.world.entity {
      it += TestWorld.placed(0f, 2f, 0f, 0f)
      it += Collider()
      it += TriggerVolume()
    }
    val hit = RayHit()

    assertFalse(test.physics.raycast(0f, 5f, 0f, 0f, -1f, 0f, 4f, hit))
    assertTrue(test.physics.raycast(0f, 5f, 0f, 0f, -1f, 0f, 6f, hit))
    assertEquals(5f, hit.distance, TOLERANCE)
    assertEquals(PhysicsWorld.NO_SURFACE, test.physics.surfaceBelow(0f, 5f, 0f, 1f))
  }

  @Test
  fun `a dynamic body falls onto the floor and writes its transform`() {
    test.floor()
    val crate =
      test.world.entity {
        it += TestWorld.placed(0f, 3f, 0f, 0f)
        it += Collider()
        it += RigidBody()
      }

    test.step(TICKS)

    assertEquals(0.5f, test.transformOf(crate).y, RESTING_TOLERANCE)
  }

  @Test
  fun `a removed collider leaves the physics world`() {
    val floor = test.floor()
    assertEquals(1, test.physics.bodyCount)

    test.world -= floor

    assertEquals(0, test.physics.bodyCount)
    assertFalse(test.physics.raycast(0f, 1f, 0f, 0f, -1f, 0f, 5f, RayHit()))
  }

  private companion object {
    const val SAND = "sand"
    const val TICKS = 180
    const val TOLERANCE = 0.02f

    /** Jolt lets a resting body sink by up to its penetration slop. */
    const val RESTING_TOLERANCE = 0.05f
  }
}

package dev.chieppa.ease.physics

import dev.chieppa.ease.physics.body.Collider
import dev.chieppa.ease.physics.body.MeshCollider
import dev.chieppa.ease.physics.body.TriangleMesh
import dev.chieppa.ease.physics.query.RayHit
import dev.chieppa.ease.physics.setup.PhysicsSettings
import dev.chieppa.ease.physics.setup.SurfaceType
import dev.chieppa.ease.physics.setup.SurfaceTypes
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Static triangle meshes collide like other colliders: characters stand on them, rays hit them. */
class MeshColliderTest {
  private val test = TestWorld(PhysicsSettings(surfaces = SurfaceTypes.of(SurfaceType("stone"))))

  @AfterTest fun close() = test.close()

  @Test
  fun `a character stands on a mesh floor and a ray reports its surface`() {
    meshEntity(floor(), surface = "stone")
    val character = test.character(0f, y = 1f)

    test.step(SECONDS_2)

    assertEquals(0f, test.transformOf(character).y, TOLERANCE)
    val hit = RayHit()
    assertTrue(test.physics.raycast(2f, 3f, 2f, 0f, -1f, 0f, 10f, hit))
    assertEquals(3f, hit.distance, TOLERANCE)
    assertEquals(1, hit.surface)
  }

  @Test
  fun `a mesh wall stops a character`() {
    meshEntity(floor())
    // A wall across x = 2, facing -x, toward the character.
    meshEntity(TriangleMesh(floatArrayOf(2f, 0f, -5f, 2f, 0f, 5f, 2f, 3f, 5f, 2f, 3f, -5f), QUAD))
    val character = test.character(0f)

    test.walk(character, x = 2f)
    test.step(SECONDS_2)

    assertTrue(test.transformOf(character).x < 2f, "x ${test.transformOf(character).x}")
  }

  @Test
  fun `removing the mesh collider removes its body`() {
    val floor = meshEntity(floor())
    test.step()
    test.world -= floor
    test.step()

    assertFalse(test.physics.raycast(0f, 3f, 0f, 0f, -1f, 0f, 10f, RayHit()))
  }

  @Test
  fun `a mesh collider added to an entity with a collider is refused`() {
    val entity =
      test.world.entity {
        it += TestWorld.placed(0f, 0f, 0f, 0f)
        it += Collider()
      }

    assertFailsWith<IllegalArgumentException> {
      with(test.world) { entity.configure { it += MeshCollider(floor()) } }
    }
  }

  @Test
  fun `a collider added to an entity with a mesh collider is refused`() {
    val entity = meshEntity(floor())

    assertFailsWith<IllegalArgumentException> {
      with(test.world) { entity.configure { it += Collider() } }
    }
  }

  @Test
  fun `replacing the mesh collider rebuilds the body`() {
    val floor = meshEntity(floor())
    test.step()
    val hit = RayHit()
    assertTrue(test.physics.raycast(2f, 3f, 2f, 0f, -1f, 0f, 10f, hit))
    assertEquals(0, hit.surface)

    with(test.world) {
      floor.configure { it += MeshCollider(raisedFloor(), surface = "stone") }
    }
    test.step()

    assertTrue(test.physics.raycast(2f, 3f, 2f, 0f, -1f, 0f, 10f, hit))
    assertEquals(2f, hit.distance, TOLERANCE)
    assertEquals(1, hit.surface)
  }

  private fun meshEntity(mesh: TriangleMesh, surface: String = "") =
    test.world.entity {
      it += TestWorld.placed(0f, 0f, 0f, 0f)
      it += MeshCollider(mesh, surface = surface)
    }

  /** A square 20 units across at y = 0, facing up. */
  private fun floor() =
    TriangleMesh(
      floatArrayOf(-10f, 0f, -10f, -10f, 0f, 10f, 10f, 0f, 10f, 10f, 0f, -10f),
      QUAD,
    )

  /** A square 20 units across at y = 1, facing up. */
  private fun raisedFloor() =
    TriangleMesh(
      floatArrayOf(-10f, 1f, -10f, -10f, 1f, 10f, 10f, 1f, 10f, 10f, 1f, -10f),
      QUAD,
    )

  private companion object {
    val QUAD = intArrayOf(0, 1, 2, 0, 2, 3)
    const val SECONDS_2 = 120
    const val TOLERANCE = 0.05f
  }
}

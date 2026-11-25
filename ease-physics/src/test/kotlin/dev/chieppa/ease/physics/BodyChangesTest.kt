package dev.chieppa.ease.physics

import dev.chieppa.ease.physics.body.Collider
import dev.chieppa.ease.physics.body.RigidBody
import dev.chieppa.ease.physics.character.CharacterController
import dev.chieppa.ease.physics.query.RayHit
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Bodies and characters follow their components after they are added: a collider's new size, a
 * rigid body added later, a controller's new height.
 */
class BodyChangesTest {
  private val test = TestWorld()
  private val hit = RayHit()

  @AfterTest fun close() = test.close()

  private fun topBelow(x: Float): Float {
    assertTrue(test.physics.raycast(x, HIGH, 0f, 0f, -1f, 0f, HIGH * 2, hit))
    return hit.y
  }

  @Test
  fun `a resized collider is the new size from the next step`() {
    val box = test.box(0f, 0f, 0f, 1f, 1f, 1f)
    test.step()
    assertEquals(1f, topBelow(0f), TOLERANCE)

    with(test.world) { box[Collider] }.halfY = 2f
    test.step()

    assertEquals(2f, topBelow(0f), TOLERANCE)
  }

  @Test
  fun `a collider given a rigid body later starts falling`() {
    test.floor()
    val crate = test.box(0f, HIGH, 0f, 0.5f, 0.5f, 0.5f)
    test.step(TICKS)
    assertEquals(HIGH, test.transformOf(crate).y, TOLERANCE)

    with(test.world) { crate.configure { it += RigidBody() } }
    test.step(TICKS)

    assertEquals(0.5f, test.transformOf(crate).y, RESTING)
  }

  @Test
  fun `a character made shorter fits under a beam it hit before`() {
    test.floor()
    // A beam whose underside is 1.5 above the floor, across the character's path.
    test.box(2f, 2f, 0f, 0.25f, 0.5f, 5f)
    val character = test.character(0f)
    test.walk(character, x = 2f)
    test.step(TICKS)
    assertTrue(test.transformOf(character).x < 2f, "blocked at full height")

    with(test.world) { character[CharacterController] }.height = 1.2f
    test.step(TICKS)

    assertTrue(test.transformOf(character).x > 3f, "walks under it shorter")
  }

  private companion object {
    const val HIGH = 6f
    const val TICKS = 120
    const val TOLERANCE = 1e-3f
    const val RESTING = 0.05f
  }
}

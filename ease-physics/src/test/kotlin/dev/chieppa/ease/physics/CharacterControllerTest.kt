package dev.chieppa.ease.physics

import dev.chieppa.ease.physics.character.CharacterMotion
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * A character walks up slopes and steps, stops at walls and steep slopes, and falls to the ground.
 */
class CharacterControllerTest {
  private val test = TestWorld()

  @AfterTest fun close() = test.close()

  @Test
  fun `a character dropped above the floor falls and stands on it`() {
    test.floor()
    val character = test.character(0f, y = 1f)

    test.step(SECONDS_2)

    assertEquals(0f, test.transformOf(character).y, TOLERANCE)
    assertEquals(0, test.motionOf(character).groundSurface)
  }

  @Test
  fun `a character walks up a gentle slope`() {
    test.floor()
    // A ramp rising 30 degrees toward +x, meeting the floor near x = 3.8.
    test.box(4f, 0f, 0f, 3f, 0.1f, 2f, degreesAboutZ = 30f)
    val character = test.character(0f)

    test.walk(character, x = 2f)
    test.step(SECONDS_3)

    val transform = test.transformOf(character)
    assertTrue(transform.x > 5f, "walked onto the ramp, x ${transform.x}")
    assertTrue(transform.y > 0.6f, "climbed it, y ${transform.y}")
    assertTrue(test.motionOf(character).groundSurface >= 0, "standing on it")
  }

  @Test
  fun `a character does not walk up a slope steeper than its steepest`() {
    test.floor()
    test.box(4f, 0f, 0f, 3f, 0.1f, 2f, degreesAboutZ = 60f)
    val character = test.character(0f)

    test.walk(character, x = 2f)
    test.step(SECONDS_3)

    assertTrue(test.transformOf(character).y < 0.3f, "y ${test.transformOf(character).y}")
  }

  @Test
  fun `a character walks up a step no taller than its tallest`() {
    test.floor()
    test.box(3f, 0.15f, 0f, 1f, 0.15f, 2f)
    val character = test.character(0f)

    test.walk(character, x = 2f)
    test.step(SECONDS_2)

    val transform = test.transformOf(character)
    assertTrue(transform.x > 2.2f, "x ${transform.x}")
    assertEquals(0.3f, transform.y, TOLERANCE)
  }

  @Test
  fun `a wall stops a character, which slides along it`() {
    test.floor()
    test.box(2f, 1f, 0f, 0.25f, 1f, 5f)
    val character = test.character(0f)

    test.walk(character, x = 2f, z = 1f)
    test.step(SECONDS_2)

    val transform = test.transformOf(character)
    assertTrue(transform.x < 2f - 0.25f - 0.29f, "stopped before the wall, x ${transform.x}")
    assertTrue(transform.z > 1.5f, "slid along it, z ${transform.z}")
  }

  @Test
  fun `a character in the air has no ground surface`() {
    val character = test.character(0f, y = 5f)

    test.step()

    assertEquals(CharacterMotion.NO_SURFACE, test.motionOf(character).groundSurface)
    assertTrue(test.motionOf(character).velocityY < 0f)
  }

  private companion object {
    const val SECONDS_2 = 120
    const val SECONDS_3 = 180
    const val TOLERANCE = 0.05f
  }
}

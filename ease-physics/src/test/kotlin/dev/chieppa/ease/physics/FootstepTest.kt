package dev.chieppa.ease.physics

import dev.chieppa.ease.physics.character.CharacterController
import dev.chieppa.ease.physics.setup.PhysicsSettings
import dev.chieppa.ease.physics.setup.SurfaceType
import dev.chieppa.ease.physics.setup.SurfaceTypes
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * A walking character reports a footstep each stride with the surface it walks on, and each change
 * of the surface under it, as a footstep sound would follow them.
 */
class FootstepTest {
  private val surfaces = SurfaceTypes.of(SurfaceType(GRASS), SurfaceType(STONE, friction = 0.6f))
  private val test = TestWorld(PhysicsSettings(surfaces = surfaces))
  private val events = RecordingListener().also { test.physics.events.add(it) }
  private val grass = surfaces.indexOf(GRASS)
  private val stone = surfaces.indexOf(STONE)

  @AfterTest fun close() = test.close()

  @Test
  fun `footsteps follow the surface type of the ground`() {
    // Grass for x below 5, stone beyond.
    test.box(-20f, -0.5f, 0f, 25f, 0.5f, 5f, surface = GRASS)
    test.box(30f, -0.5f, 0f, 25f, 0.5f, 5f, surface = STONE)
    val character = test.character(0f)
    val stride = CharacterController.DEFAULT_STRIDE

    test.walk(character, x = 2f)
    test.step(TICKS)

    val walked = test.transformOf(character).x
    assertEquals((walked / stride).toInt(), events.footsteps.size, "a step each stride")
    val onGrass = events.footsteps.takeWhile { it == grass }.size
    assertEquals((BORDER / stride).toInt(), onGrass, "steps on grass up to x = 5")
    assertTrue(events.footsteps.drop(onGrass).all { it == stone }, "then on stone")
    assertEquals(listOf(grass, stone), events.groundChanges)
    assertEquals(stone, test.motionOf(character).groundSurface)
  }

  @Test
  fun `a character standing still takes no steps`() {
    test.floor()
    test.character(0f)

    test.step(TICKS)

    assertEquals(emptyList(), events.footsteps)
    assertEquals(listOf(0), events.groundChanges)
  }

  private companion object {
    const val GRASS = "grass"
    const val STONE = "stone"
    const val BORDER = 5f
    const val TICKS = 300
  }
}

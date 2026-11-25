package dev.chieppa.ease.physics

import dev.chieppa.ease.physics.query.RayHit
import dev.chieppa.ease.physics.setup.CollisionLayers
import dev.chieppa.ease.physics.setup.PhysicsSettings
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** A blocking volume on a layer only players collide with stops the player and lets NPCs pass. */
class PlayerOnlyWallTest {
  private val layers = CollisionLayers.build {
    layer(WORLD)
    layer(PLAYER)
    layer(NPC)
    layer(PLAYER_WALL)
    ignore(NPC, PLAYER_WALL)
  }
  private val test = TestWorld(PhysicsSettings(layers))

  @AfterTest fun close() = test.close()

  @Test
  fun `the wall stops the player and lets the NPC through`() {
    test.floor()
    test.box(2f, 1f, 0f, 0.1f, 1f, 5f, layer = PLAYER_WALL)
    val player = test.character(0f, z = -1f, layer = PLAYER)
    val npc = test.character(0f, z = 1f, layer = NPC)

    test.walk(player, x = 2f)
    test.walk(npc, x = 2f)
    test.step(TICKS)

    assertTrue(test.transformOf(player).x < 2f, "player at ${test.transformOf(player).x}")
    assertTrue(test.transformOf(npc).x > 3f, "npc at ${test.transformOf(npc).x}")
  }

  @Test
  fun `a raycast on a layer sees only what that layer collides with`() {
    val wall = test.box(2f, 1f, 0f, 0.1f, 1f, 5f, layer = PLAYER_WALL)
    val hit = RayHit()

    val player = test.physics.raycast(0f, 1f, 0f, 1f, 0f, 0f, 10f, hit, layers.indexOf(PLAYER))
    assertTrue(player && hit.entity == wall)
    assertFalse(test.physics.raycast(0f, 1f, 0f, 1f, 0f, 0f, 10f, hit, layers.indexOf(NPC)))
  }

  private companion object {
    const val WORLD = "World"
    const val PLAYER = "Player"
    const val NPC = "Npc"
    const val PLAYER_WALL = "PlayerWall"
    const val TICKS = 120
  }
}

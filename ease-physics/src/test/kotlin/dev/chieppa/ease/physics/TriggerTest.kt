package dev.chieppa.ease.physics

import com.github.quillraven.fleks.Entity
import dev.chieppa.ease.physics.body.Collider
import dev.chieppa.ease.physics.setup.CollisionLayers
import dev.chieppa.ease.physics.setup.PhysicsSettings
import dev.chieppa.ease.physics.volume.TriggerVolume
import dev.chieppa.ease.physics.volume.VolumeSensor
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Trigger volumes report sensors entering and leaving them through the physics events: a trigger
 * that fires once reports its first entry only, others every entry and exit, and a trigger's layers
 * choose whose entries it reports. Triggers block nothing.
 */
class TriggerTest {
  private val layers = CollisionLayers.build {
    layer(PLAYER)
    layer(NPC)
  }
  private val test = TestWorld(PhysicsSettings(layers))
  private val events = RecordingListener().also { test.physics.events.add(it) }

  @AfterTest fun close() = test.close()

  private fun trigger(once: Boolean, vararg layers: String): Entity =
    test.world.entity { trigger ->
      trigger += TestWorld.placed(3f, 1f, 0f, 0f)
      trigger +=
        Collider().apply {
          halfX = 0.5f
          halfY = 1f
          halfZ = 3f
        }
      trigger +=
        TriggerVolume().apply {
          this.once = once
          this.layers = layers.toList()
        }
    }

  private fun sensingCharacter(z: Float = 0f, layer: String = PLAYER): Entity =
    test.character(0f, z = z, layer = layer) { world, entity ->
      with(world) { entity.configure { it += VolumeSensor().apply { offsetY = 0.5f } } }
    }

  /** Walks [character] through the trigger at x = 3 and back, twice. */
  private fun walkThroughTwice(characters: List<Entity>) {
    repeat(2) {
      characters.forEach { character -> test.walk(character, x = 3f) }
      test.step(SECONDS_2)
      characters.forEach { character -> test.walk(character, x = -3f) }
      test.step(SECONDS_2)
    }
  }

  @Test
  fun `a trigger that fires once reports one entry for two walks through it`() {
    test.floor()
    val trigger = trigger(once = true)
    val character = sensingCharacter()

    walkThroughTwice(listOf(character))

    assertEquals(listOf(trigger to character), events.entered)
    assertEquals(emptyList(), events.exited)
    assertTrue(with(test.world) { trigger[TriggerVolume] }.fired)
  }

  @Test
  fun `a repeating trigger reports every entry and exit, and blocks nothing`() {
    test.floor()
    val trigger = trigger(once = false)
    val character = sensingCharacter()

    test.walk(character, x = 3f)
    test.step(SECONDS_2)
    assertTrue(test.transformOf(character).x > 4f, "walked through it")
    test.walk(character, x = -3f)
    test.step(SECONDS_2)
    test.walk(character, x = 3f)
    test.step(SECONDS_2)

    assertEquals(3, events.entered.size)
    assertEquals(3, events.exited.size)
    assertTrue(events.entered.all { it == trigger to character })
  }

  @Test
  fun `a trigger reports only sensors on its layers`() {
    test.floor()
    trigger(once = false, PLAYER)
    val player = sensingCharacter(z = -1f, layer = PLAYER)
    val npc = sensingCharacter(z = 1f, layer = NPC)

    walkThroughTwice(listOf(player, npc))

    assertTrue(events.entered.isNotEmpty())
    assertTrue(events.entered.all { it.second == player })
    assertFalse(events.exited.any { it.second == npc })
  }

  private companion object {
    const val PLAYER = "Player"
    const val NPC = "Npc"
    const val SECONDS_2 = 120
  }
}

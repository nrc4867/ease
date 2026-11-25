package dev.chieppa.ease.platform.input

import dev.chieppa.ease.testing.AllocationBudget
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ActionInputTest {
  private val keyboard = ScriptedDevice(DeviceKind.KEYBOARD)
  private val pad = ScriptedDevice(DeviceKind.GAMEPAD)
  private val source =
    object : InputSource {
      override val devices: List<InputDevice> = listOf(keyboard, pad)

      override fun poll() = devices.forEach(InputDevice::poll)
    }
  private val actions =
    ActionSet(
      listOf(
        ActionDefinition("move_x", ActionKind.AXIS),
        ActionDefinition("jump", ActionKind.BUTTON),
        ActionDefinition("confirm", ActionKind.BUTTON),
      )
    )
  private val gameplay =
    InputContext("gameplay")
      .bind("move_x", "key.d")
      .bind("move_x", "key.a", -1f)
      .bind("move_x", "gamepad.left_x")
      .bind("jump", "key.space")
      .bind("jump", "gamepad.south")
  private val menu =
    InputContext("menu", blocksBelow = true)
      .bind("confirm", "key.enter")
      .bind("confirm", "gamepad.south")
  private val input = ActionInput(source, actions).also { it.contexts.push(gameplay) }
  private val moveX = actions.indexOfOrThrow("move_x")
  private val jump = actions.indexOfOrThrow("jump")
  private val confirm = actions.indexOfOrThrow("confirm")

  @Test
  fun `a keyboard and a gamepad drive the same axis`() {
    keyboard.set("key.a")
    input.update()
    assertEquals(-1f, input.value(moveX))

    keyboard.releaseAll()
    pad.set("gamepad.left_x", 0.6f)
    input.update()
    assertEquals((0.6f - DEFAULT_DEADZONE) / (1f - DEFAULT_DEADZONE), input.value(moveX), 1e-6f)
  }

  @Test
  fun `a stick inside the deadzone reads zero`() {
    pad.set("gamepad.left_x", 0.1f)
    input.update()

    assertEquals(0f, input.value(moveX))
  }

  @Test
  fun `opposing keys cancel`() {
    keyboard.set("key.a")
    keyboard.set("key.d")
    input.update()

    assertEquals(0f, input.value(moveX))
  }

  @Test
  fun `buttons report presses and releases once`() {
    keyboard.set("key.space")
    input.update()
    assertTrue(input.justPressed(jump))
    input.update()
    assertTrue(input.pressed(jump))
    assertFalse(input.justPressed(jump))

    keyboard.release("key.space")
    input.update()
    assertTrue(input.justReleased(jump))
    assertFalse(input.pressed(jump))
  }

  @Test
  fun `a blocking context hides the actions below it`() {
    input.contexts.push(menu)
    keyboard.set("key.space")
    keyboard.set("key.enter")
    input.update()

    assertFalse(input.pressed(jump))
    assertTrue(input.pressed(confirm))

    input.contexts.pop()
    input.update()
    assertTrue(input.pressed(jump))
  }

  @Test
  fun `an action bound in an upper context overrides the lower one`() {
    val special = InputContext("special").bind("jump", "key.enter")
    input.contexts.push(special)
    keyboard.set("key.space")
    input.update()
    assertFalse(input.pressed(jump))

    keyboard.set("key.enter")
    input.update()
    assertTrue(input.pressed(jump))
  }

  @Test
  fun `the active device follows the last one used and the glyph follows it`() {
    assertEquals("key.space", input.glyph(jump))

    keyboard.set("key.space")
    input.update()
    assertEquals(DeviceKind.KEYBOARD, input.activeDevice)

    keyboard.releaseAll()
    pad.set("gamepad.south")
    input.update()
    assertEquals(DeviceKind.GAMEPAD, input.activeDevice)
    assertEquals("gamepad.south", input.glyph(jump))

    keyboard.set("key.d")
    input.update()
    assertEquals("key.space", input.glyph(jump))
  }

  @Test
  fun `a glyph is missing where the contexts bind the action nowhere`() {
    input.contexts.push(menu)

    assertNull(input.glyph(jump))
    assertEquals("key.enter", input.glyph(confirm))
  }

  @Test
  fun `rebinding replaces the binding on that device only`() {
    gameplay.rebind("jump", "key.w")

    assertEquals(
      listOf("gamepad.south", "key.w"),
      gameplay.bindings.filter { it.action == "jump" }.map { it.control }.sorted(),
    )
    keyboard.set("key.w")
    input.update()
    assertTrue(input.pressed(jump))
  }

  @Test
  fun `updating and reading allocate nothing`() {
    val keys = KeyboardDevice { it == 7 }
    val steady =
      ActionInput(
        object : InputSource {
          override val devices: List<InputDevice> = listOf(keys)

          override fun poll() = keys.poll()
        },
        actions,
      )
    steady.contexts.push(gameplay)

    AllocationBudget.assertNoAllocation {
      steady.update()
      steady.value(moveX)
      steady.pressed(jump)
      steady.justPressed(jump)
    }
  }
}

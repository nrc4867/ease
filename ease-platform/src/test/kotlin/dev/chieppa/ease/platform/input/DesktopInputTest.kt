package dev.chieppa.ease.platform.input

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DesktopInputTest {
  private class FakePad(override val name: String) : GamepadHandle {
    val down = mutableSetOf<Int>()
    val axes = FloatArray(AXIS_COUNT)

    override fun isDown(button: Int): Boolean = button in down

    override fun axis(axis: Int): Float = axes[axis]
  }

  private class FakePads : GamepadProvider {
    var connected: List<GamepadHandle> = emptyList()

    override fun update() = Unit

    override fun scan(): List<GamepadHandle> = connected
  }

  private val keysDown = mutableSetOf<Int>()
  private val pads = FakePads()
  private val input = DesktopInput({ it in keysDown }, pads)

  @Test
  fun `the keyboard reads by control id`() {
    keysDown += SCANCODE_SPACE
    input.poll()

    assertEquals(1f, input.devices.single().value("key.space"))
    assertEquals(0f, input.devices.single().value("key.a"))
    assertTrue(input.devices.single().active)
  }

  @Test
  fun `a gamepad reads buttons and sticks with up positive, and joins when plugged in`() {
    input.poll()
    assertEquals(1, input.devices.size)

    val pad = FakePad("Test pad")
    pad.down += BUTTON_SOUTH
    pad.axes[AXIS_LEFT_Y] = 1f
    pads.connected = listOf(pad)
    repeat(RESCAN_EVERY) { input.poll() }

    val device = input.devices.single { it.kind == DeviceKind.GAMEPAD }
    assertEquals("Test pad", device.name)
    assertEquals(1f, device.value("gamepad.south"))
    assertEquals(-1f, device.value("gamepad.left_y"))
  }

  @Test
  fun `an unplugged gamepad leaves`() {
    pads.connected = listOf(FakePad("Test pad"))
    input.poll()
    assertEquals(2, input.devices.size)

    pads.connected = emptyList()
    repeat(RESCAN_EVERY) { input.poll() }

    assertEquals(1, input.devices.size)
  }

  private companion object {
    const val SCANCODE_SPACE = 44
    const val BUTTON_SOUTH = 0
    const val AXIS_LEFT_Y = 1
    const val AXIS_COUNT = 6
  }
}

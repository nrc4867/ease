package dev.chieppa.ease.platform.input

/** A control of a gamepad: its id, whether it is an axis, its SDL number and the sign applied. */
private class GamepadControl(val id: String, val isAxis: Boolean, val number: Int, val sign: Float)

private val BUTTONS =
  listOf(
    "south",
    "east",
    "west",
    "north",
    "back",
    "guide",
    "start",
    "left_stick",
    "right_stick",
    "left_shoulder",
    "right_shoulder",
    "dpad_up",
    "dpad_down",
    "dpad_left",
    "dpad_right",
  )

private val AXES =
  listOf("left_x" to 1f, "left_y" to -1f, "right_x" to 1f, "right_y" to -1f) +
    listOf("left_trigger" to 1f, "right_trigger" to 1f)

private val CONTROLS =
  BUTTONS.mapIndexed { number, id -> GamepadControl("gamepad.$id", false, number, 1f) } +
    AXES.mapIndexed { number, (id, sign) -> GamepadControl("gamepad.$id", true, number, sign) }

/** The id of every control a [GamepadDevice] reads. */
internal val GAMEPAD_CONTROLS: List<String> = CONTROLS.map { it.id }

/**
 * One gamepad. Buttons: `gamepad.south`, `east`, `west`, `north` (the face buttons by position, so
 * glyphs can follow the model), `back`, `guide`, `start`, `left_stick`, `right_stick`,
 * `left_shoulder`, `right_shoulder`, `dpad_up`, `dpad_down`, `dpad_left` and `dpad_right`. Axes:
 * `left_x`, `left_y`, `right_x` and `right_y` (up and right positive), `left_trigger` and
 * `right_trigger`.
 */
public class GamepadDevice(private val handle: GamepadHandle) : SampledDevice(GAMEPAD_CONTROLS) {
  override val kind: DeviceKind = DeviceKind.GAMEPAD
  override val name: String
    get() = handle.name

  override fun read(index: Int): Float {
    val control = CONTROLS[index]
    if (control.isAxis) return handle.axis(control.number) * control.sign
    return if (handle.isDown(control.number)) 1f else 0f
  }
}

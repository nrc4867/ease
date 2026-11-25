package dev.chieppa.ease.platform.input

import kotlin.math.abs

/** How far a stick or trigger must move before it reads; the rest rescales to fill 0 to 1. */
public const val DEFAULT_DEADZONE: Float = 0.15f

/**
 * The action layer: reads the devices of [source] each [update] and answers by action, through the
 * bindings of the contexts on [contexts]. Game code asks for actions and axes, never buttons. It
 * also tracks the [activeDevice], the kind of device the player touched last, so a UI shows that
 * device's glyphs (see [glyph]) and switches the moment the player picks up another.
 *
 * [update], [value], [pressed] and the edge reads allocate nothing once the devices are set up.
 */
public class ActionInput(
  private val source: InputSource,
  public val actions: ActionSet,
  public val contexts: InputContextStack = InputContextStack(),
  private val deadzone: Float = DEFAULT_DEADZONE,
) {
  private val count = actions.actions.size
  private val values = FloatArray(count)
  private val before = FloatArray(count)
  private val claimed = BooleanArray(count)

  /** The kind of device the player used last, or null before any input. */
  public var activeDevice: DeviceKind? = null
    private set

  /** Polls the devices and works out every action's value from the contexts in force. */
  public fun update() {
    source.poll()
    trackActiveDevice()
    values.copyInto(before)
    values.fill(0f)
    claimed.fill(false)
    for (level in contexts.size - 1 downTo 0) {
      val context = contexts[level]
      applyBindings(context)
      if (context.blocksBelow) break
    }
  }

  /** The value of action [action]: -1 to 1 for an axis, 0 or 1 for a button. */
  public fun value(action: Int): Float = values[action]

  /** Whether action [action] is held. */
  public fun pressed(action: Int): Boolean = values[action] >= PRESS_THRESHOLD

  /** Whether action [action] became held at the last [update]. */
  public fun justPressed(action: Int): Boolean = pressed(action) && before[action] < PRESS_THRESHOLD

  /** Whether action [action] was let go at the last [update]. */
  public fun justReleased(action: Int): Boolean =
    !pressed(action) && before[action] >= PRESS_THRESHOLD

  /**
   * The id of the control that triggers action [action] on the [activeDevice] (the keyboard when
   * none is active yet), such as `key.space` or `gamepad.south`, for a UI to draw a glyph; null
   * when the contexts in force bind it nowhere on that device. Control ids name positions, not
   * brands.
   */
  public fun glyph(action: Int): String? {
    val kind = activeDevice ?: DeviceKind.KEYBOARD
    val name = actions.actions[action].name
    for (level in contexts.size - 1 downTo 0) {
      val context = contexts[level]
      val bound = context.bindings.firstOrNull { it.action == name }
      if (bound != null) {
        return context.bindings
          .firstOrNull {
            it.action == name && DeviceKind.of(it.control) == kind
          }
          ?.control
      }
      if (context.blocksBelow) break
    }
    return null
  }

  private fun trackActiveDevice() {
    val devices = source.devices
    for (index in devices.indices) if (devices[index].active) activeDevice = devices[index].kind
  }

  private fun applyBindings(context: InputContext) {
    val bindings = context.bindings
    for (index in bindings.indices) {
      val binding = bindings[index]
      val action = actions.indexOf(binding.action)
      if (action < 0 || claimed[action]) continue
      values[action] = combine(action, values[action], read(binding))
    }
    // Contexts above own the actions they bind, so contexts below never add to them.
    for (index in bindings.indices) {
      val action = actions.indexOf(bindings[index].action)
      if (action >= 0) claimed[action] = true
    }
  }

  private fun combine(action: Int, current: Float, reading: Float): Float =
    if (actions.actions[action].kind == ActionKind.AXIS) {
      (current + reading).coerceIn(-1f, 1f)
    } else if (reading >= PRESS_THRESHOLD) 1f else current

  /** What [binding] reads now: the strongest reading among the devices, scaled. */
  private fun read(binding: Binding): Float {
    var strongest = 0f
    val devices = source.devices
    for (index in devices.indices) {
      val raw = devices[index].value(binding.control)
      if (abs(raw) > abs(strongest)) strongest = raw
    }
    return applyDeadzone(strongest) * binding.scale
  }

  private fun applyDeadzone(raw: Float): Float {
    val size = abs(raw)
    if (size <= deadzone) return 0f
    val rescaled = (size - deadzone) / (1f - deadzone)
    return if (raw < 0f) -rescaled else rescaled
  }
}

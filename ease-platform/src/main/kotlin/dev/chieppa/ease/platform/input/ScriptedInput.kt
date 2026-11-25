package dev.chieppa.ease.platform.input

import kotlin.math.abs

/**
 * A device a script or a recording drives instead of hardware: it reads whatever was last [set],
 * and counts as emulating a keyboard or gamepad by its [kind].
 */
public class ScriptedDevice(
  override val kind: DeviceKind,
  override val name: String = "Scripted ${kind.name.lowercase()}",
) : InputDevice {
  private val held = HashMap<String, Float>()
  private val seen = HashMap<String, Float>()

  override var active: Boolean = false
    private set

  /** Holds [control] at [value] until it is set again or released. */
  public fun set(control: String, value: Float = 1f) {
    held[control] = value
  }

  /** Lets go of [control]. */
  public fun release(control: String) {
    held[control] = 0f
  }

  /** Lets go of every control. */
  public fun releaseAll() {
    for (control in held.keys) held[control] = 0f
  }

  override fun poll() {
    var activity = false
    for ((control, value) in held) {
      val before = seen[control] ?: 0f
      if (abs(value) >= PRESS_THRESHOLD && abs(before) < PRESS_THRESHOLD) {
        activity = true
      }
      seen[control] = value
    }
    active = activity
  }

  override fun value(control: String): Float = seen[control] ?: 0f
}

/** A [ScriptedDevice] set as an [InputSource] on its own, for tests and headless runs. */
public class ScriptedInput(public val device: ScriptedDevice) : InputSource {
  override val devices: List<InputDevice> = listOf(device)

  override fun poll(): Unit = device.poll()
}

/**
 * A recording replayed through a [ScriptedDevice]: each poll applies the next frame, a map of the
 * controls held in that tick, and every control is released once the frames run out.
 */
public class RecordedInput(
  private val frames: List<Map<String, Float>>,
  kind: DeviceKind = DeviceKind.KEYBOARD,
) : InputSource {
  private val device = ScriptedDevice(kind)
  private var next = 0

  override val devices: List<InputDevice> = listOf(device)

  override fun poll() {
    device.releaseAll()
    if (next < frames.size) {
      for ((control, value) in frames[next]) device.set(control, value)
    }
    next++
    device.poll()
  }
}

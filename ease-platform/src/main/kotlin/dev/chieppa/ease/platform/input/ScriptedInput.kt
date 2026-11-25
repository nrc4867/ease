package dev.chieppa.ease.platform.input

import kotlin.math.abs

/**
 * A device a script or a recording drives instead of hardware: it reads whatever was last [set],
 * and counts as emulating a keyboard or gamepad by its [kind]. Setting a control it has not seen
 * allocates once; after that, setting, releasing and polling allocate nothing.
 */
public class ScriptedDevice(
  override val kind: DeviceKind,
  override val name: String = "Scripted ${kind.name.lowercase()}",
) : InputDevice {
  private val indices = HashMap<String, Int>()
  private var held = FloatArray(INITIAL_CONTROLS)
  private var seen = FloatArray(INITIAL_CONTROLS)
  private var count = 0

  override var active: Boolean = false
    private set

  /** Holds [control] at [value] until it is set again or released. */
  public fun set(control: String, value: Float = 1f) {
    held[indexOf(control)] = value
  }

  /** Lets go of [control]. */
  public fun release(control: String) {
    held[indexOf(control)] = 0f
  }

  /** Lets go of every control. */
  public fun releaseAll() {
    held.fill(0f, 0, count)
  }

  override fun poll() {
    var activity = false
    for (index in 0 until count) {
      if (abs(held[index]) >= PRESS_THRESHOLD && abs(seen[index]) < PRESS_THRESHOLD) {
        activity = true
      }
      seen[index] = held[index]
    }
    active = activity
  }

  override fun value(control: String): Float {
    val index = indices[control] ?: return 0f
    return seen[index]
  }

  private fun indexOf(control: String): Int {
    indices[control]?.let {
      return it
    }
    if (count == held.size) {
      held = held.copyOf(count * 2)
      seen = seen.copyOf(count * 2)
    }
    indices[control] = count
    return count++
  }

  private companion object {
    const val INITIAL_CONTROLS = 16
  }
}

/** A [ScriptedDevice] set as an [InputSource] on its own, for tests and headless runs. */
public class ScriptedInput(public val device: ScriptedDevice) : InputSource {
  override val devices: List<InputDevice> = listOf(device)

  override fun poll(): Unit = device.poll()
}

/**
 * A recording replayed through a [ScriptedDevice]: each poll applies the next frame, a map of the
 * controls held in that tick, and every control is released once the frames run out. Polling
 * allocates nothing.
 */
public class RecordedInput(
  frames: List<Map<String, Float>>,
  kind: DeviceKind = DeviceKind.KEYBOARD,
) : InputSource {
  private val device = ScriptedDevice(kind)
  private val controls = frames.flatMap { it.keys }.distinct()
  private val levels =
    Array(frames.size) { tick -> FloatArray(controls.size) { frames[tick][controls[it]] ?: 0f } }
  private var next = 0

  override val devices: List<InputDevice> = listOf(device)

  init {
    for (control in controls) device.release(control)
  }

  override fun poll() {
    device.releaseAll()
    if (next < levels.size) {
      val frame = levels[next]
      for (index in frame.indices) if (frame[index] != 0f) device.set(controls[index], frame[index])
    }
    next++
    device.poll()
  }
}

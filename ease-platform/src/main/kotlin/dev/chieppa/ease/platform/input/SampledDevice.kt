package dev.chieppa.ease.platform.input

/** The value from which a control counts as pressed, for activity and for button actions. */
public const val PRESS_THRESHOLD: Float = 0.5f

/**
 * A device whose controls are a fixed list of ids, sampled into an array on each [poll]. Subclasses
 * say how one control is read; reading and activity tracking allocate nothing.
 */
public abstract class SampledDevice(controls: List<String>) : InputDevice {
  private val indices: Map<String, Int> = controls.withIndex().associate { it.value to it.index }
  private val values = FloatArray(controls.size)

  final override var active: Boolean = false
    private set

  /** The current reading of control number [index], in the order of the ids given. */
  protected abstract fun read(index: Int): Float

  final override fun poll() {
    var activity = false
    for (index in values.indices) {
      val before = values[index]
      val now = read(index)
      values[index] = now
      if (now >= PRESS_THRESHOLD && before < PRESS_THRESHOLD) activity = true
      if (now <= -PRESS_THRESHOLD && before > -PRESS_THRESHOLD) activity = true
    }
    active = activity
  }

  final override fun value(control: String): Float {
    val index = indices[control] ?: return 0f
    return values[index]
  }
}

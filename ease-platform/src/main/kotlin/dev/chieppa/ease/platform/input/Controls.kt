package dev.chieppa.ease.platform.input

/** The control ids the devices of this engine read. */
public object Controls {
  private val known: Set<String> = (KEYBOARD_CONTROLS + GAMEPAD_CONTROLS).toSet()

  /** Whether [control] names a key or gamepad control some device reads. */
  public fun isKnown(control: String): Boolean = control in known
}

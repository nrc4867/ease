package dev.chieppa.ease.platform.input

/** The family of an input device, which decides the glyphs a UI shows for it. */
public enum class DeviceKind(
  /** The prefix every control id of this kind starts with, such as `key.`. */
  public val prefix: String
) {
  /** A keyboard: control ids `key.<name>`. */
  KEYBOARD("key."),

  /** A gamepad: control ids `gamepad.<name>`. */
  GAMEPAD("gamepad.");

  public companion object {
    /** The kind a [control] id belongs to, or null when it names no kind. */
    public fun of(control: String): DeviceKind? = entries.firstOrNull {
      control.startsWith(it.prefix)
    }
  }
}

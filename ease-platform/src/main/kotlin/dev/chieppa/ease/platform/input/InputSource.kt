package dev.chieppa.ease.platform.input

/** The devices a game reads, which may come and go (gamepads plugged in and out). */
public interface InputSource {
  /** The devices present now. */
  public val devices: List<InputDevice>

  /** Reads every device's state; call once per frame or tick before reading values. */
  public fun poll()
}

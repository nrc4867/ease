package dev.chieppa.ease.platform.input

/** Finds the gamepads that are connected. */
public interface GamepadProvider {
  /** Reads fresh state for every connected gamepad. */
  public fun update()

  /** Looks for gamepads plugged in or out; the answer lists those connected now. */
  public fun scan(): List<GamepadHandle>
}

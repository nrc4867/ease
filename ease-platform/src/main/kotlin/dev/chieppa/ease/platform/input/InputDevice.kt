package dev.chieppa.ease.platform.input

/**
 * One physical (or scripted) device, read by control id such as `key.space`, `gamepad.south` or
 * `gamepad.left_x`. Buttons read 0 or 1; analog controls read -1 to 1 (triggers 0 to 1, sticks with
 * up and right positive).
 */
public interface InputDevice {
  /** The family this device belongs to. */
  public val kind: DeviceKind

  /** A name to show the player, such as the gamepad's model. */
  public val name: String

  /** Whether the last [poll] saw a control newly pressed or pushed far from rest. */
  public val active: Boolean

  /** Reads the device's current state. */
  public fun poll()

  /** The value of [control] as of the last [poll]; 0 for a control this device does not have. */
  public fun value(control: String): Float
}

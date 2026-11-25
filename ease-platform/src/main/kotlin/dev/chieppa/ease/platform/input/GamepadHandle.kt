package dev.chieppa.ease.platform.input

/** One connected gamepad as the platform reports it. */
public interface GamepadHandle {
  /** The model name. */
  public val name: String

  /** Whether the button with SDL gamepad button number [button] is held. */
  public fun isDown(button: Int): Boolean

  /** The axis with SDL gamepad axis number [axis], -1 to 1 (triggers 0 to 1). Down is positive. */
  public fun axis(axis: Int): Float
}

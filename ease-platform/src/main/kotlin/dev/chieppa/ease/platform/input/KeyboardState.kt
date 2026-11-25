package dev.chieppa.ease.platform.input

/** Which keys are down, as the window system reports them. */
public fun interface KeyboardState {
  /** Whether the key with SDL scancode [scancode] is held. */
  public fun isDown(scancode: Int): Boolean
}

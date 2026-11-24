package dev.chieppa.ease.platform

/** Something that happened to a [Window] since its events were last polled. */
public sealed interface WindowEvent {
  /** The user asked to close the window, with its close button or the system's shortcut. */
  public data object CloseRequested : WindowEvent

  /** The window's drawable area is now [size]. */
  public data class Resized(val size: PixelSize) : WindowEvent
}

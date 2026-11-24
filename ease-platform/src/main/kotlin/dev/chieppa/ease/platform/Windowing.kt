package dev.chieppa.ease.platform

/**
 * Opens windows and reports what happens to them. Make it, and call everything on it and its
 * windows, from the process's first thread: macOS allows windows nowhere else, and the other
 * desktops are held to the same rule so code that runs on one runs on all.
 */
public interface Windowing : AutoCloseable {
  /** Opens a window as [spec] describes and shows it. */
  public fun open(spec: WindowSpec): Window

  /** The events since the last call, for every open window, oldest first. */
  public fun pollEvents(): List<Pair<Window, WindowEvent>>
}

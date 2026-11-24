package dev.chieppa.ease.platform

/**
 * The native handles a GPU API needs to present into a window, as raw addresses. Each case is what
 * one windowing system hands out; the GPU layer turns it into a surface.
 */
public sealed interface SurfaceHandle {
  /** A Windows window: its module instance and its `HWND`. */
  public data class Win32(val hinstance: Long, val hwnd: Long) : SurfaceHandle

  /** A macOS view's `CAMetalLayer`. */
  public data class Metal(val layer: Long) : SurfaceHandle

  /** An X11 window: the `Display` connection and the window's id. */
  public data class Xlib(val display: Long, val window: Long) : SurfaceHandle

  /** A Wayland surface: the `wl_display` and the `wl_surface`. */
  public data class Wayland(val display: Long, val surface: Long) : SurfaceHandle
}

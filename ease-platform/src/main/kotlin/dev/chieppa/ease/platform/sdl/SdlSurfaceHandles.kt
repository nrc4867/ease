package dev.chieppa.ease.platform.sdl

import dev.chieppa.ease.platform.SurfaceHandle
import org.lwjgl.sdl.SDLMetal.SDL_Metal_CreateView
import org.lwjgl.sdl.SDLMetal.SDL_Metal_GetLayer
import org.lwjgl.sdl.SDLProperties.SDL_GetNumberProperty
import org.lwjgl.sdl.SDLProperties.SDL_GetPointerProperty
import org.lwjgl.sdl.SDLVideo.SDL_GetCurrentVideoDriver
import org.lwjgl.sdl.SDLVideo.SDL_GetWindowProperties
import org.lwjgl.sdl.SDLVideo.SDL_PROP_WINDOW_WAYLAND_DISPLAY_POINTER
import org.lwjgl.sdl.SDLVideo.SDL_PROP_WINDOW_WAYLAND_SURFACE_POINTER
import org.lwjgl.sdl.SDLVideo.SDL_PROP_WINDOW_WIN32_HWND_POINTER
import org.lwjgl.sdl.SDLVideo.SDL_PROP_WINDOW_WIN32_INSTANCE_POINTER
import org.lwjgl.sdl.SDLVideo.SDL_PROP_WINDOW_X11_DISPLAY_POINTER
import org.lwjgl.sdl.SDLVideo.SDL_PROP_WINDOW_X11_WINDOW_NUMBER

/** SDL's names for the video drivers whose windows a GPU surface can present into. */
private object VideoDriver {
  const val WINDOWS = "windows"
  const val COCOA = "cocoa"
  const val X11 = "x11"
  const val WAYLAND = "wayland"
}

/** What a GPU surface needs from an SDL window: its native handles, and on macOS its Metal view. */
internal class SdlSurface(val handle: SurfaceHandle, val metalView: Long?)

/**
 * The native handles of [window], read from its SDL properties. On macOS it gives the window a
 * Metal view, whose layer is the handle; the view lives until the window is destroyed.
 */
internal fun sdlSurface(window: Long): SdlSurface {
  val properties = SDL_GetWindowProperties(window)
  fun pointer(name: String): Long =
    SDL_GetPointerProperty(properties, name, 0L).also {
      check(it != 0L) { "SDL window has no $name" }
    }
  return when (val driver = SDL_GetCurrentVideoDriver()) {
    VideoDriver.WINDOWS ->
      SdlSurface(
        SurfaceHandle.Win32(
          pointer(SDL_PROP_WINDOW_WIN32_INSTANCE_POINTER),
          pointer(SDL_PROP_WINDOW_WIN32_HWND_POINTER),
        ),
        metalView = null,
      )
    VideoDriver.COCOA -> {
      val view = SDL_Metal_CreateView(window)
      check(view != 0L) { "SDL could not make a Metal view: ${sdlError()}" }
      SdlSurface(SurfaceHandle.Metal(SDL_Metal_GetLayer(view)), view)
    }
    VideoDriver.X11 ->
      SdlSurface(
        SurfaceHandle.Xlib(
          pointer(SDL_PROP_WINDOW_X11_DISPLAY_POINTER),
          SDL_GetNumberProperty(properties, SDL_PROP_WINDOW_X11_WINDOW_NUMBER, 0L),
        ),
        metalView = null,
      )
    VideoDriver.WAYLAND ->
      SdlSurface(
        SurfaceHandle.Wayland(
          pointer(SDL_PROP_WINDOW_WAYLAND_DISPLAY_POINTER),
          pointer(SDL_PROP_WINDOW_WAYLAND_SURFACE_POINTER),
        ),
        metalView = null,
      )
    else -> error("SDL video driver $driver has no GPU surface")
  }
}

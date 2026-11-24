package dev.chieppa.ease.platform.sdl

import dev.chieppa.ease.platform.PixelSize
import dev.chieppa.ease.platform.Window
import dev.chieppa.ease.platform.WindowEvent
import dev.chieppa.ease.platform.WindowSpec
import dev.chieppa.ease.platform.Windowing
import org.lwjgl.sdl.SDLEvents.SDL_EVENT_WINDOW_CLOSE_REQUESTED
import org.lwjgl.sdl.SDLEvents.SDL_EVENT_WINDOW_PIXEL_SIZE_CHANGED
import org.lwjgl.sdl.SDLEvents.SDL_PollEvent
import org.lwjgl.sdl.SDLInit.SDL_INIT_VIDEO
import org.lwjgl.sdl.SDLInit.SDL_Init
import org.lwjgl.sdl.SDLInit.SDL_QuitSubSystem
import org.lwjgl.sdl.SDLVideo.SDL_CreateWindow
import org.lwjgl.sdl.SDLVideo.SDL_GetWindowID
import org.lwjgl.sdl.SDLVideo.SDL_WINDOW_HIGH_PIXEL_DENSITY
import org.lwjgl.sdl.SDLVideo.SDL_WINDOW_METAL
import org.lwjgl.sdl.SDLVideo.SDL_WINDOW_RESIZABLE
import org.lwjgl.sdl.SDL_Event

/** Desktop windows through SDL3: Windows, macOS, and X11 or Wayland on Linux. */
public class SdlWindowing : Windowing {
  private val windows = mutableMapOf<Int, SdlWindow>()
  private val event = SDL_Event.create()
  private val metal = System.getProperty("os.name").lowercase().startsWith("mac")

  init {
    check(SDL_Init(SDL_INIT_VIDEO)) { "SDL video failed to start: ${sdlError()}" }
  }

  override fun open(spec: WindowSpec): Window {
    var flags = SDL_WINDOW_HIGH_PIXEL_DENSITY
    if (spec.resizable) flags = flags or SDL_WINDOW_RESIZABLE
    if (metal) flags = flags or SDL_WINDOW_METAL
    val native = SDL_CreateWindow(spec.title, spec.width, spec.height, flags)
    check(native != 0L) { "SDL could not open a window: ${sdlError()}" }
    val id = SDL_GetWindowID(native)
    return SdlWindow(native) { windows.remove(id) }.also { windows[id] = it }
  }

  override fun pollEvents(): List<Pair<Window, WindowEvent>> {
    val events = mutableListOf<Pair<Window, WindowEvent>>()
    while (SDL_PollEvent(event)) {
      val window = windows[event.window().windowID()] ?: continue
      when (event.type()) {
        SDL_EVENT_WINDOW_CLOSE_REQUESTED -> events += window to WindowEvent.CloseRequested
        SDL_EVENT_WINDOW_PIXEL_SIZE_CHANGED -> {
          val size = PixelSize(event.window().data1(), event.window().data2())
          events += window to WindowEvent.Resized(size)
        }
      }
    }
    return events
  }

  override fun close() {
    windows.values.toList().forEach(Window::close)
    SDL_QuitSubSystem(SDL_INIT_VIDEO)
  }
}

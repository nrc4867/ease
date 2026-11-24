package dev.chieppa.ease.platform.sdl

import dev.chieppa.ease.platform.PixelSize
import dev.chieppa.ease.platform.SurfaceHandle
import dev.chieppa.ease.platform.Window
import org.lwjgl.sdl.SDLMetal.SDL_Metal_DestroyView
import org.lwjgl.sdl.SDLVideo.SDL_DestroyWindow
import org.lwjgl.sdl.SDLVideo.SDL_GetWindowSizeInPixels
import org.lwjgl.sdl.SDLVideo.SDL_SetWindowSize
import org.lwjgl.system.MemoryStack

/** An SDL window and the native handles of its surface. */
internal class SdlWindow(private val window: Long, private val onClose: (SdlWindow) -> Unit) :
  Window {
  private val native = sdlSurface(window)
  private var closed = false

  override val surface: SurfaceHandle
    get() = native.handle

  override val pixelSize: PixelSize
    get() =
      MemoryStack.stackPush().use { stack ->
        val width = stack.mallocInt(1)
        val height = stack.mallocInt(1)
        check(SDL_GetWindowSizeInPixels(window, width, height)) { sdlError() }
        PixelSize(width[0], height[0])
      }

  override fun requestSize(width: Int, height: Int) {
    check(SDL_SetWindowSize(window, width, height)) { sdlError() }
  }

  override fun close() {
    if (closed) return
    closed = true
    native.metalView?.let(::SDL_Metal_DestroyView)
    SDL_DestroyWindow(window)
    onClose(this)
  }
}

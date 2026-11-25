package dev.chieppa.ease.platform.sdl

import dev.chieppa.ease.platform.input.DesktopInput
import dev.chieppa.ease.platform.input.GamepadHandle
import dev.chieppa.ease.platform.input.GamepadProvider
import dev.chieppa.ease.platform.input.InputDevice
import dev.chieppa.ease.platform.input.InputSource
import java.nio.ByteBuffer
import org.lwjgl.sdl.SDLGamepad.SDL_CloseGamepad
import org.lwjgl.sdl.SDLGamepad.SDL_GetGamepadAxis
import org.lwjgl.sdl.SDLGamepad.SDL_GetGamepadButton
import org.lwjgl.sdl.SDLGamepad.SDL_GetGamepadName
import org.lwjgl.sdl.SDLGamepad.SDL_GetGamepads
import org.lwjgl.sdl.SDLGamepad.SDL_OpenGamepad
import org.lwjgl.sdl.SDLGamepad.SDL_UpdateGamepads
import org.lwjgl.sdl.SDLHints.SDL_HINT_JOYSTICK_ALLOW_BACKGROUND_EVENTS
import org.lwjgl.sdl.SDLHints.SDL_SetHint
import org.lwjgl.sdl.SDLInit.SDL_INIT_GAMEPAD
import org.lwjgl.sdl.SDLInit.SDL_Init
import org.lwjgl.sdl.SDLInit.SDL_QuitSubSystem
import org.lwjgl.sdl.SDLKeyboard.SDL_GetKeyboardState
import org.lwjgl.sdl.SDLStdinc.SDL_free

private const val AXIS_MAX = 32767f

/** One SDL gamepad opened for reading. */
private class SdlGamepad(private val native: Long, val id: Int) : GamepadHandle {
  override val name: String = SDL_GetGamepadName(native) ?: "Gamepad"

  override fun isDown(button: Int): Boolean = SDL_GetGamepadButton(native, button)

  override fun axis(axis: Int): Float =
    (SDL_GetGamepadAxis(native, axis) / AXIS_MAX).coerceIn(-1f, 1f)

  fun close() = SDL_CloseGamepad(native)
}

/** The gamepads SDL sees; opens each once and closes it when it is unplugged. */
private class SdlGamepadProvider : GamepadProvider {
  private val open = mutableListOf<SdlGamepad>()

  override fun update() = SDL_UpdateGamepads()

  override fun scan(): List<GamepadHandle> {
    val ids = SDL_GetGamepads()
    val present = mutableListOf<Int>()
    if (ids != null) {
      while (ids.hasRemaining()) present += ids.get()
      SDL_free(ids)
    }
    val gone = open.filter { it.id !in present }
    gone.forEach { it.close() }
    open.removeAll(gone)
    for (id in present) {
      if (open.none { it.id == id })
        SDL_OpenGamepad(id).takeIf { it != 0L }?.let { open += SdlGamepad(it, id) }
    }
    return open
  }

  fun close() = open.forEach(SdlGamepad::close)
}

/**
 * The keyboard and gamepads through SDL3. Call [poll] from the thread that pumps window events (see
 * `SdlWindowing.pollEvents`), which also keeps SDL's keyboard state current. Gamepads keep
 * reporting while the window is in the background.
 */
public class SdlInput private constructor(private val provider: SdlGamepadProvider) :
  InputSource, AutoCloseable {
  private val keys: ByteBuffer? = SDL_GetKeyboardState()
  private val desktop =
    DesktopInput(
      { code -> keys != null && code < keys.limit() && keys.get(code).toInt() != 0 },
      provider,
    )

  override val devices: List<InputDevice>
    get() = desktop.devices

  override fun poll(): Unit = desktop.poll()

  override fun close() {
    provider.close()
    SDL_QuitSubSystem(SDL_INIT_GAMEPAD)
  }

  public companion object {
    /** Starts SDL's gamepad support and reads the keyboard and every gamepad. */
    public fun open(): SdlInput {
      SDL_SetHint(SDL_HINT_JOYSTICK_ALLOW_BACKGROUND_EVENTS, "1")
      check(SDL_Init(SDL_INIT_GAMEPAD)) { "SDL gamepads failed to start: ${sdlError()}" }
      return SdlInput(SdlGamepadProvider())
    }
  }
}

package dev.chieppa.ease.platform.sdl

import org.lwjgl.sdl.SDLError.SDL_GetError

/** SDL's message for the last call that failed on this thread. */
internal fun sdlError(): String = SDL_GetError() ?: "no message"

package dev.chieppa.ease.platform.smoke

import dev.chieppa.ease.platform.WindowSpec
import dev.chieppa.ease.platform.sdl.SdlWindowing

/** How many times events are drained after each change, so the window system can catch up. */
private const val EVENT_ROUNDS = 10

private const val WIDTH = 640
private const val HEIGHT = 360

/**
 * Opens a window, reads its native handle and size, resizes it, and closes it. Any step that fails
 * throws, so the process exits non-zero.
 */
public fun main() {
  SdlWindowing().use { windowing ->
    val window = windowing.open(WindowSpec("Ease window smoke", WIDTH, HEIGHT))
    repeat(EVENT_ROUNDS) { windowing.pollEvents() }
    println("Opened: ${window.surface}, ${window.pixelSize} pixels")
    check(!window.pixelSize.isEmpty) { "The window has no drawable area" }
    window.requestSize(WIDTH * 2, HEIGHT * 2)
    val events = List(EVENT_ROUNDS) { windowing.pollEvents() }.flatten()
    println("After resize: ${window.pixelSize} pixels, events $events")
    window.close()
  }
  println("Closed cleanly")
}

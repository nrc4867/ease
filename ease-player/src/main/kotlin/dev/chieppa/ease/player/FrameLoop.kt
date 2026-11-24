package dev.chieppa.ease.player

import dev.chieppa.ease.gpu.CommandEncoder
import dev.chieppa.ease.gpu.GpuContext
import dev.chieppa.ease.gpu.TrianglePass
import dev.chieppa.ease.platform.Window
import dev.chieppa.ease.platform.WindowEvent
import dev.chieppa.ease.platform.Windowing

/** How far the triangle turns each frame, in radians. */
private const val TURN_PER_FRAME = 0.02f

/** The window size a counted run asks for halfway, in screen units. */
private const val RESIZED_WIDTH = 1440
private const val RESIZED_HEIGHT = 810

/**
 * Draws [window]'s frames with [gpu]: the triangle, turning a step each frame. Presenting waits for
 * the display, and while the window is minimized it waits for window events, so it never spins.
 */
internal class FrameLoop(
  private val windowing: Windowing,
  private val window: Window,
  private val gpu: GpuContext,
  private val triangle: TrianglePass,
) {
  private val surface = checkNotNull(gpu.surface) { "The GPU context has no window surface" }

  /**
   * Draws until the window is asked to close, or [frames] frames with a resize halfway, and answers
   * how many it drew.
   */
  fun run(frames: Int?): Int {
    surface.configure(window.pixelSize)
    var frame = 0
    while (frames == null || frame < frames) {
      if (!handleEvents()) break
      if (frames != null && frame == frames / 2) window.requestSize(RESIZED_WIDTH, RESIZED_HEIGHT)
      if (draw(frame)) frame++
    }
    return frame
  }

  /** Applies the window's events; false once it is asked to close. */
  private fun handleEvents(): Boolean {
    for ((_, event) in windowing.pollEvents()) {
      when (event) {
        WindowEvent.CloseRequested -> return false
        is WindowEvent.Resized -> surface.configure(event.size)
      }
    }
    return true
  }

  /** Draws and presents frame [frame]; false when there was no frame to draw into. */
  private fun draw(frame: Int): Boolean {
    val target = surface.acquire()
    if (target == null) {
      // Minimized, or out of date: wait for the window to change, or match its size.
      if (window.pixelSize.isEmpty) windowing.awaitEvent() else surface.configure(window.pixelSize)
      return false
    }
    val encoder = CommandEncoder.on(gpu)
    triangle.record(encoder, target, frame * TURN_PER_FRAME)
    encoder.submit()
    surface.present()
    gpu.checkErrors()
    return true
  }
}

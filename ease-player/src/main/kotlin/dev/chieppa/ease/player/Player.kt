package dev.chieppa.ease.player

import dev.chieppa.ease.gpu.GpuContext
import dev.chieppa.ease.gpu.TrianglePass
import dev.chieppa.ease.platform.WindowSpec
import dev.chieppa.ease.platform.sdl.SdlWindowing

private const val WIDTH = 960
private const val HEIGHT = 540

/**
 * Opens the game window and draws into it until it is closed, or for a set number of frames. Runs
 * on the process's first thread, as windows need.
 */
public fun main(args: Array<String>) {
  val options = PlayerOptions.parse(args)
  SdlWindowing().use { windowing ->
    val window = windowing.open(WindowSpec("Ease", WIDTH, HEIGHT))
    GpuContext.open(window.surface).use { gpu ->
      println("Ease player on ${gpu.description}")
      TrianglePass(gpu).use { triangle ->
        val frames = FrameLoop(windowing, window, gpu, triangle).run(options.frames)
        println("Drew $frames frames")
      }
    }
  }
}

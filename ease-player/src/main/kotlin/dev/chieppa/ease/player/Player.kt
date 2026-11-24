package dev.chieppa.ease.player

import dev.chieppa.ease.gpu.GpuContext
import dev.chieppa.ease.platform.WindowSpec
import dev.chieppa.ease.platform.sdl.SdlWindowing
import dev.chieppa.ease.platform.time.SystemClock
import dev.chieppa.ease.render.WorldRenderer
import dev.chieppa.ease.runtime.GameModules
import dev.chieppa.ease.runtime.HostedWorld
import dev.chieppa.ease.runtime.WorldRunner
import dev.chieppa.ease.runtime.loop.FixedStep

private const val WIDTH = 960
private const val HEIGHT = 540

/**
 * Runs the one game on the classpath in a window until it is closed, or for a set number of frames:
 * its world ticks on the fixed-step loop in real time and is drawn every frame. Runs on the
 * process's first thread, as windows need.
 */
public fun main(args: Array<String>) {
  val options = PlayerOptions.parse(args)
  val game = GameModules.load()
  val world = HostedWorld(game, FixedStep.perSecond())
  val runner = WorldRunner(world, SystemClock)
  SdlWindowing().use { windowing ->
    val window = windowing.open(WindowSpec("Ease", WIDTH, HEIGHT))
    GpuContext.open(window.surface).use { gpu ->
      println("Ease player running ${game.javaClass.simpleName} on ${gpu.description}")
      WorldRenderer(gpu).use { renderer ->
        val frames =
          FrameLoop(windowing, window, gpu, runner, renderer, game.camera()).run(options.frames)
        println("Drew $frames frames over ${world.tick} ticks")
      }
    }
  }
}

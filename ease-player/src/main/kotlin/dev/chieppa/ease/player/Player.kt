package dev.chieppa.ease.player

import dev.chieppa.ease.gpu.GpuContext
import dev.chieppa.ease.platform.WindowSpec
import dev.chieppa.ease.platform.sdl.SdlWindowing
import dev.chieppa.ease.platform.time.SystemClock
import dev.chieppa.ease.render.WorldRenderer
import dev.chieppa.ease.runtime.GameModule
import dev.chieppa.ease.runtime.GameModules
import dev.chieppa.ease.runtime.HostedWorld
import dev.chieppa.ease.runtime.WorldRunner
import dev.chieppa.ease.runtime.loop.FixedStep

private const val WIDTH = 960
private const val HEIGHT = 540

/**
 * Runs the one game on the classpath, with its input script attached: in a window until it is
 * closed or for a set number of frames, its world ticking on the fixed-step loop in real time and
 * drawn every frame; or headless for a set number of ticks. Runs on the process's first thread, as
 * windows need.
 */
public fun main(args: Array<String>) {
  val options = PlayerOptions.parse(args)
  val game = GameModules.load()
  val world = HostedWorld(game, FixedStep.perSecond(), input = game.inputScript())
  val ticks = options.headlessTicks
  if (ticks != null) {
    runHeadless(world, ticks)
  } else {
    runWindowed(game, world, options.frames)
  }
}

/** Runs [ticks] ticks of [world] with no window, GPU or clock, and reports what changed it. */
private fun runHeadless(world: HostedWorld, ticks: Int) {
  repeat(ticks) { world.step() }
  val log = world.history.log
  val commands = log.all.size
  println("Ran ${world.tick} ticks headless; $commands commands logged")
}

private fun runWindowed(game: GameModule, world: HostedWorld, frames: Int?) {
  val runner = WorldRunner(world, SystemClock)
  SdlWindowing().use { windowing ->
    val window = windowing.open(WindowSpec("Ease", WIDTH, HEIGHT))
    GpuContext.open(window.surface).use { gpu ->
      println("Ease player running ${game.javaClass.simpleName} on ${gpu.description}")
      WorldRenderer(gpu).use { renderer ->
        val drawn = FrameLoop(windowing, window, gpu, runner, renderer, game.camera()).run(frames)
        println("Drew $drawn frames over ${world.tick} ticks")
      }
    }
  }
}

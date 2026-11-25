package dev.chieppa.ease.player

import dev.chieppa.ease.gpu.GpuContext
import dev.chieppa.ease.platform.WindowSpec
import dev.chieppa.ease.platform.sdl.SdlWindowing
import dev.chieppa.ease.platform.time.SystemClock
import dev.chieppa.ease.render.WorldRenderer
import dev.chieppa.ease.runtime.GameContent
import dev.chieppa.ease.runtime.GameModule
import dev.chieppa.ease.runtime.GameModules
import dev.chieppa.ease.runtime.LocalHost
import dev.chieppa.ease.runtime.WorldRunner
import dev.chieppa.ease.runtime.loop.FixedStep

private const val WIDTH = 960
private const val HEIGHT = 540

/**
 * Runs the one game on the classpath as a host world plus a local client world, with its input
 * script attached to the client, over the game's content folder when `--content` names one: in a
 * window until it is closed or for a set number of frames, both ticking on the fixed-step loop in
 * real time and the client's world drawn every frame; or headless for a set number of ticks.
 * Single-player is this same host and client. Runs on the process's first thread, as windows need.
 */
public fun main(args: Array<String>) {
  val options = PlayerOptions.parse(args)
  val game = GameModules.load()
  val assets = options.content?.let { GameContent.open(it) }
  val local = LocalHost(game, FixedStep.perSecond(), input = game.inputScript(), assets = assets)
  val ticks = options.headlessTicks
  if (ticks != null) {
    runHeadless(local, ticks)
  } else {
    runWindowed(game, local, options.frames)
  }
}

/**
 * Runs [ticks] ticks of [local] with no window, GPU or clock, and reports what changed the host's
 * world, what its scenes placed, and whether the client kept up. Fails when the client's session
 * closed.
 */
private fun runHeadless(local: LocalHost, ticks: Int) {
  repeat(ticks) { local.step() }
  val host = local.host
  val log = host.history.log
  val commands = log.all.size
  val client = local.client.session
  check(client.isOpen) { "The local client's session closed: ${client.closeReason}" }
  val scenes = host.scenes?.scenes.orEmpty()
  val clientWorld = local.client.world.world
  println(
    "Ran ${local.tick} ticks headless; $commands commands logged; " +
      "${scenes.sumOf { it.keys.size }} scene nodes from ${scenes.size} scenes; " +
      "host ${host.world.numEntities} entities, " +
      "client ${clientWorld.numEntities} at snapshot ${client.latestSnapshotTick}"
  )
}

private fun runWindowed(game: GameModule, local: LocalHost, frames: Int?) {
  val runner = WorldRunner(local, SystemClock)
  SdlWindowing().use { windowing ->
    val window = windowing.open(WindowSpec("Ease", WIDTH, HEIGHT))
    GpuContext.open(window.surface).use { gpu ->
      println("Ease player running ${game.javaClass.simpleName} on ${gpu.description}")
      WorldRenderer(gpu).use { renderer ->
        val drawn = FrameLoop(windowing, window, gpu, runner, renderer, game.camera()).run(frames)
        println("Drew $drawn frames over ${local.tick} ticks")
      }
    }
  }
}

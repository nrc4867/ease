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
  val assets = GameContent.forGame(game, options.content)
  val local = LocalHost(game, FixedStep.perSecond(), input = game.inputScript(), assets = assets)
  local.sceneProblems = { System.err.println("Scene problem: ${it.message}") }
  val ticks = options.headlessTicks
  if (ticks != null) {
    runHeadless(local, ticks)
  } else {
    runWindowed(game, local, options.frames)
  }
}

/**
 * Runs [ticks] ticks of [local] with no window, GPU or clock, and reports what changed the host's
 * world, what its scenes placed and could not honor, and whether the client kept up. Fails when a
 * scene holds a component of a type the game does not register, or when the client's session
 * closed.
 */
private fun runHeadless(local: LocalHost, ticks: Int) {
  repeat(ticks) { local.step() }
  val host = local.host
  host.sceneProblems.checkTypes()
  val log = host.history.log
  val commands = log.all.size
  val client = local.client.session
  check(client.isOpen) { "The local client's session closed: ${client.closeReason}" }
  val scenes = host.scenes?.scenes.orEmpty()
  val clientWorld = local.client.world.world
  println(
    "Ran ${local.tick} ticks headless; $commands commands logged; " +
      "${scenes.sumOf { it.keys.size }} scene nodes from ${scenes.size} scenes, " +
      "${host.sceneProblems.count} scene problems; " +
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

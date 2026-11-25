package dev.chieppa.ease.sample

import dev.chieppa.ease.commands.CommandRegistry
import dev.chieppa.ease.commands.builtin.Spawn
import dev.chieppa.ease.ecs.Phase
import dev.chieppa.ease.ecs.PhasedSystems
import dev.chieppa.ease.ecs.replication.Authority
import dev.chieppa.ease.ecs.replication.PeerId
import dev.chieppa.ease.render.Camera2D
import dev.chieppa.ease.runtime.GameModule
import dev.chieppa.ease.runtime.HostedWorld
import dev.chieppa.ease.runtime.InputScript

/**
 * The sample game: a few colored squares bouncing around the [Playfield], and for each peer that
 * joins a courier square it owns, which its input moves, predicted on its own client.
 */
public class SampleGame : GameModule {
  override fun registerTypes(registry: CommandRegistry) {
    registry.component(Velocity::class, Velocity.serializer())
    registry.component(Courier::class, Courier.serializer())
    registry.replicated(VelocityReplicated)
    registry.replicated(CourierReplicated)
    registry.command(SetPace::class, SetPace.serializer())
  }

  override fun predictedSystems(systems: PhasedSystems) {
    systems.add(Phase.SIMULATION, CourierSystem())
  }

  override fun hostSystems(systems: PhasedSystems) {
    systems.add(Phase.SIMULATION, BounceSystem())
  }

  override fun populate(world: HostedWorld) {
    SQUARES.forEach { world.execute(Spawn(it.components())) }
  }

  /** Gives [peer] a courier it owns, at the center, standing still until its input moves it. */
  override fun peerJoined(world: HostedWorld, peer: PeerId) {
    val courier = CourierScript.COURIER.components() + Courier() + Authority(PeerId.HOST, peer)
    world.execute(Spawn(courier))
  }

  override fun inputScript(): InputScript = CourierScript()

  override fun camera(): Camera2D = Camera2D(visibleHeight = Playfield.VISIBLE_HEIGHT)

  public companion object {
    /** The squares the game starts with, far enough apart that none covers another at first. */
    public val SQUARES: List<Square> =
      listOf(
        Square(x = -5f, y = 2f, speedX = 3f, speedY = 1.5f, size = 1.2f, rgb = 0xF2735A),
        Square(x = 4f, y = -2f, speedX = -2f, speedY = 2.5f, size = 1f, rgb = 0x33BFB3),
        Square(x = 0f, y = 0f, speedX = 1.5f, speedY = -3f, size = 1.5f, rgb = 0xF2CC40),
        Square(x = -3f, y = -3f, speedX = -2.5f, speedY = -1f, size = 0.8f, rgb = 0x9966E6),
        Square(x = 5f, y = 3f, speedX = -3.5f, speedY = -0.5f, size = 0.9f, rgb = 0x73CC4D),
      )
  }
}

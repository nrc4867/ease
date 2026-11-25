package dev.chieppa.ease.runtime

import dev.chieppa.ease.commands.CommandRegistry
import dev.chieppa.ease.ecs.PhasedSystems
import dev.chieppa.ease.ecs.replication.PeerId
import dev.chieppa.ease.render.Camera2D

/**
 * A game's code as hosts see it. A game lists its module in
 * `META-INF/services/dev.chieppa.ease.runtime.GameModule`, and a host finds it with [GameModules]
 * and builds a [HostedWorld] from it.
 */
public interface GameModule {
  /** Registers the game's own command and component types, so worlds can log and send them. */
  public fun registerTypes(registry: CommandRegistry) {}

  /**
   * Adds the game's systems, each in its phase. Systems are constructed here, where the world being
   * built is current.
   */
  public fun systems(systems: PhasedSystems) {}

  /**
   * Creates the game's starting entities in the host's [world], through [HostedWorld.execute] so
   * the history and the command log hold them. Clients receive them as spawns.
   */
  public fun populate(world: HostedWorld) {}

  /**
   * Gives the peer [peer], whose join the host's world [world] just accepted, what it starts with,
   * such as a character it owns, through [HostedWorld.execute]. Runs on the host only, before the
   * tick the peer joined at.
   */
  public fun peerJoined(world: HostedWorld, peer: PeerId) {}

  /**
   * The game's scripted input, for demos and headless runs, or null when it has none. Hosts choose
   * whether to attach it to a [ClientWorld], where it sends commands to the host.
   */
  public fun inputScript(): InputScript? = null

  /** The camera a host starts with. */
  public fun camera(): Camera2D = Camera2D()
}

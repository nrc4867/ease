package dev.chieppa.ease.runtime

import dev.chieppa.ease.commands.CommandRegistry
import dev.chieppa.ease.ecs.PhasedSystems
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
   * Creates the game's starting entities in [world], through [HostedWorld.execute] so the history
   * and the command log hold them.
   */
  public fun populate(world: HostedWorld) {}

  /** The camera a host starts with. */
  public fun camera(): Camera2D = Camera2D()
}

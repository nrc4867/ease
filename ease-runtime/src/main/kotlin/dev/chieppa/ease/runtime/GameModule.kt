package dev.chieppa.ease.runtime

import dev.chieppa.ease.assets.Guid
import dev.chieppa.ease.assets.associations.ContributorProvider
import dev.chieppa.ease.commands.CommandRegistry
import dev.chieppa.ease.ecs.PhasedSystems
import dev.chieppa.ease.ecs.replication.PeerId
import dev.chieppa.ease.render.Camera2D
import dev.chieppa.ease.scene.types.SceneTypes

/**
 * A game's code as hosts see it. A game lists its module in
 * `META-INF/services/dev.chieppa.ease.runtime.GameModule`, and a host finds it with [GameModules]
 * and builds a [HostedWorld] from it. A game that registers importers also lists the module under
 * `ContributorProvider` (see [registerContributors]).
 */
public interface GameModule : ContributorProvider {
  /**
   * Registers the game's own command and component types, so worlds can log and send them. A
   * component type its scenes save goes in [registerSceneTypes] instead, which registers it here
   * too; its replicated serializer is still registered here.
   */
  public fun registerTypes(registry: CommandRegistry) {}

  /**
   * Registers the component types the game's scene and prefab files hold, beyond the engine's, each
   * with the codec as well, so a world spawns them from files, saves them and sends them.
   */
  public fun registerSceneTypes(types: SceneTypes.Builder) {}

  /**
   * The GUID of the scene the host's world starts from, or null when the game builds its world in
   * code alone. The host finds it in the game's content and places it before [populate]; changes to
   * its files reload into the running world. Named by GUID, so moving or renaming the scene keeps
   * it, as it keeps every other reference to it.
   */
  public fun startScene(): Guid? = null

  /**
   * Adds the systems every world runs, the host's and each client's, each in its phase: systems
   * that read replicated state or keep state of their own that never replicates, such as
   * presentation and effects. A client holds replicas it may not write, so a system that changes
   * replicated state goes in [predictedSystems] or [hostSystems]. Systems are constructed here,
   * where the world being built is current.
   */
  public fun systems(systems: PhasedSystems) {}

  /**
   * Adds the systems that move what a peer controls from its input, such as a character controller,
   * after [systems] in each phase. They run in every world: the host's runs them for every peer's
   * entities, and each client runs them ahead of the host for the entities it owns, from its own
   * input, then again from the host's state when the host disagrees. They read input from
   * `World.peerInputs`, which on a client holds only its own, and check `Replication.mayWrite`
   * before writing an entity, so the same code runs everywhere and writes only what this world may:
   * on a client, only its own entities.
   */
  public fun predictedSystems(systems: PhasedSystems) {}

  /**
   * Adds the systems only the host's world runs, after [predictedSystems] in each phase: the
   * simulation, which writes replicated state and spawns entities. Clients receive what it does as
   * spawns and snapshots, so these systems never check authority on a client's behalf.
   */
  public fun hostSystems(systems: PhasedSystems) {}

  /**
   * Creates the game's starting entities in the host's [world], through [HostedWorld.execute] so
   * the history and the command log hold them, after the [startScene] is placed. Clients receive
   * them as spawns.
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

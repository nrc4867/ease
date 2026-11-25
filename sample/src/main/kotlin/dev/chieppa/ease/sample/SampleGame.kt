package dev.chieppa.ease.sample

import dev.chieppa.ease.assets.Guid
import dev.chieppa.ease.assets.associations.FileAssociations
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
import dev.chieppa.ease.scene.types.SceneTypes

/**
 * The sample game: the colored squares of its saved playfield scene bouncing around the
 * [Playfield], and for each peer that joins a courier square it owns, which its input moves,
 * predicted on its own client. The scene is the game's content, `content/playfield.scene`; editing
 * it while the game runs changes the running squares. Couriers are not in the scene: one exists per
 * joined peer, owned by that peer, which a scene shared by every peer cannot say.
 */
public class SampleGame : GameModule {
  override fun registerSceneTypes(types: SceneTypes.Builder) {
    types.component(Velocity, Velocity.serializer())
  }

  override fun registerContributors(associations: FileAssociations) {
    PaletteImporter.register(associations)
  }

  override fun registerTypes(registry: CommandRegistry) {
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

  override fun startScene(): Guid = PLAYFIELD

  /** Gives [peer] a courier it owns, at the center, standing still until its input moves it. */
  override fun peerJoined(world: HostedWorld, peer: PeerId) {
    val courier = CourierScript.COURIER.components() + Courier() + Authority(PeerId.HOST, peer)
    world.execute(Spawn(courier))
  }

  override fun inputScript(): InputScript = CourierScript()

  override fun camera(): Camera2D = Camera2D(visibleHeight = Playfield.VISIBLE_HEIGHT)

  public companion object {
    /**
     * The playfield scene the game starts from: its squares, as `playfield.scene.meta` names it.
     */
    public val PLAYFIELD: Guid = Guid.parse("f98b920b-b589-4932-b442-424c23eedb69")
  }
}

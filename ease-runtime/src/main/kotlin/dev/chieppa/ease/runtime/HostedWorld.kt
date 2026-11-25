package dev.chieppa.ease.runtime

import com.github.quillraven.fleks.World
import dev.chieppa.ease.assets.loading.AssetStore
import dev.chieppa.ease.assets.reload.AssetReloadSystem
import dev.chieppa.ease.assets.reload.AssetSync
import dev.chieppa.ease.commands.Command
import dev.chieppa.ease.commands.CommandHistory
import dev.chieppa.ease.commands.CommandOrigin
import dev.chieppa.ease.commands.WorldCodec
import dev.chieppa.ease.ecs.DEFAULT_ENTITY_CAPACITY
import dev.chieppa.ease.ecs.Phase
import dev.chieppa.ease.ecs.phasedWorld
import dev.chieppa.ease.ecs.prediction.CorrectionOffsets
import dev.chieppa.ease.ecs.replication.PeerId
import dev.chieppa.ease.ecs.replication.ReplicationSettings
import dev.chieppa.ease.net.input.PEER_INPUTS_INJECTABLE
import dev.chieppa.ease.net.input.PeerInputs
import dev.chieppa.ease.render.ExtractSystem
import dev.chieppa.ease.render.RenderSnapshot
import dev.chieppa.ease.runtime.loop.FixedStep
import dev.chieppa.ease.scene.runtime.SceneNodes
import dev.chieppa.ease.scene.runtime.SceneSync

private const val NANOS_PER_SECOND = 1_000_000_000f

/**
 * One world a host runs, as [peer]: a Fleks world with [module]'s systems and predicted systems
 * (its host systems too in the host's own world) and the engine's [ExtractSystem], the codec and
 * command history every outside change goes through, each peer's [inputs], the [snapshot] rendering
 * reads, drawn [corrections] away while a prediction settles, and the current [tick]. Every world
 * holds a `SceneNodes` system with the module's scene types. Building it registers [module]'s types
 * and, in the host's own world, places the module's start scene from [assets] and lets the module
 * populate it; a client's world starts empty and receives the host's entities.
 *
 * With [assets], each [step] of the host's world begins by applying the asset file changes waiting
 * since the last one, as `ReloadAsset` commands in the history, then the changes they make to the
 * placed [scenes], as node commands, so a changed file reloads in the running world and reaches
 * clients as spawns, snapshots and despawns.
 *
 * A [replaying] world is one the host's command log is replayed into: it places no scene, does not
 * populate and runs neither sync, since the log holds every command those made; its [assets] only
 * serve the log's `ReloadAsset` commands, which need the files at the versions they name.
 */
@Suppress("LongParameterList") // Named arguments with defaults; most worlds pass two.
public class HostedWorld(
  module: GameModule,
  public val fixedStep: FixedStep,
  entityCapacity: Int = DEFAULT_ENTITY_CAPACITY,
  public val peer: PeerId = PeerId.HOST,
  assets: AssetStore? = null,
  public val corrections: CorrectionOffsets = CorrectionOffsets(),
  replaying: Boolean = false,
) {
  private val types = WorldTypes(module)

  /** What the last tick's extract copied out for rendering. */
  public val snapshot: RenderSnapshot = RenderSnapshot(entityCapacity)

  /** Reads and writes this world's commands and state, the module's types included. */
  public val codec: WorldCodec = types.codec

  /**
   * Each peer's input for the tick being run, which the world's systems read as `World.peerInputs`:
   * on the host every joined peer's, on a client only its own.
   */
  public val inputs: PeerInputs = PeerInputs()

  /**
   * The entities, components and systems, the codec's replicated types replicating. Change it from
   * outside only through [execute].
   */
  public val world: World =
    phasedWorld(
      entityCapacity,
      ReplicationSettings(peer, codec.replicated),
      mapOf(PEER_INPUTS_INJECTABLE to inputs),
    ) {
      add(Phase.INPUT, SceneNodes(types.scene))
      module.systems(this)
      predicted { module.predictedSystems(this) }
      if (peer == PeerId.HOST) module.hostSystems(this)
      assets?.let { add(Phase.INPUT, AssetReloadSystem(it)) }
      add(Phase.EXTRACT, ExtractSystem(snapshot, corrections))
    }

  /** Every change made through [execute], with undo, redo and the command log. */
  public val history: CommandHistory = CommandHistory(world, codec)

  /** The tick the next [step] runs, which is also the number of steps run so far. */
  public var tick: Long = 0
    private set

  private val syncs = peer == PeerId.HOST && !replaying

  private val assetSync: AssetSync? =
    assets?.takeIf { syncs }?.let { AssetSync(it.database, it, history) }

  /**
   * The scenes placed in the host's world and kept in step with their files, or null on a client,
   * in a replaying world and without [assets].
   */
  public val scenes: SceneSync? = assets?.takeIf { syncs }?.let { SceneSync(world, it, history) }

  /** How long one step is, in seconds. */
  internal val stepSeconds = fixedStep.stepNanos / NANOS_PER_SECOND

  init {
    if (syncs) start(module, assets)
  }

  /** Places [module]'s start scene from [assets], then lets the module populate the world. */
  private fun start(module: GameModule, assets: AssetStore?) {
    val scene = module.startScene()
    if (scene != null) {
      val name = module.javaClass.simpleName
      checkNotNull(assets) { "$name starts from scene $scene, so its host needs the content" }
      checkNotNull(assets.database[scene]) { "$name's start scene $scene is not in its content" }
      checkNotNull(scenes).place(scene, tick)
    }
    module.populate(this)
  }

  /**
   * Runs one tick: the asset reloads and the scene changes they bring, then every system, phase by
   * phase, one fixed step long.
   */
  public fun step() {
    assetSync?.beforeTick(tick)
    scenes?.beforeTick(tick)
    world.update(stepSeconds)
    tick++
  }

  /** Moves the tick on to [tick] without running the ticks between, as a client's clock does. */
  internal fun skipTo(tick: Long) {
    require(tick >= this.tick) { "A world's tick only moves on: $tick is before ${this.tick}" }
    this.tick = tick
  }

  /**
   * Applies [command] through the history, stamped with the current tick and [origin]. Throws
   * `CommandRejected`, changing nothing, when it cannot apply.
   */
  public fun execute(command: Command, origin: CommandOrigin = CommandOrigin.GAME) {
    history.execute(command, tick, origin)
  }
}

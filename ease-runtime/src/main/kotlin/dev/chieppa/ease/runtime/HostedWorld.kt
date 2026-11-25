package dev.chieppa.ease.runtime

import com.github.quillraven.fleks.World
import dev.chieppa.ease.assets.loading.AssetStore
import dev.chieppa.ease.assets.reload.AssetReloadSystem
import dev.chieppa.ease.assets.reload.AssetSync
import dev.chieppa.ease.assets.reload.registerAssetCommands
import dev.chieppa.ease.commands.Command
import dev.chieppa.ease.commands.CommandHistory
import dev.chieppa.ease.commands.CommandOrigin
import dev.chieppa.ease.commands.WorldCodec
import dev.chieppa.ease.commands.worldCodec
import dev.chieppa.ease.ecs.DEFAULT_ENTITY_CAPACITY
import dev.chieppa.ease.ecs.Phase
import dev.chieppa.ease.ecs.phasedWorld
import dev.chieppa.ease.ecs.prediction.CorrectionOffsets
import dev.chieppa.ease.ecs.replication.PeerId
import dev.chieppa.ease.ecs.replication.ReplicationSettings
import dev.chieppa.ease.net.input.PEER_INPUTS_INJECTABLE
import dev.chieppa.ease.net.input.PeerInputs
import dev.chieppa.ease.render.ExtractSystem
import dev.chieppa.ease.render.Quad
import dev.chieppa.ease.render.QuadReplicated
import dev.chieppa.ease.render.RenderSnapshot
import dev.chieppa.ease.runtime.loop.FixedStep

private const val NANOS_PER_SECOND = 1_000_000_000f

/**
 * One world a host runs, as [peer]: a Fleks world with [module]'s systems and predicted systems
 * (its host systems too in the host's own world) and the engine's [ExtractSystem], the codec and
 * command history every outside change goes through, each peer's [inputs], the [snapshot] rendering
 * reads, drawn [corrections] away while a prediction settles, and the current [tick]. Building it
 * registers [module]'s types and, in the host's own world, lets the module populate it; a client's
 * world starts empty and receives the host's entities. With [assets], each [step] begins by
 * applying the asset file changes waiting since the last one, as `ReloadAsset` commands in the
 * history, so a changed file reloads in the running world.
 */
public class HostedWorld(
  module: GameModule,
  public val fixedStep: FixedStep,
  entityCapacity: Int = DEFAULT_ENTITY_CAPACITY,
  public val peer: PeerId = PeerId.HOST,
  assets: AssetStore? = null,
  public val corrections: CorrectionOffsets = CorrectionOffsets(),
) {
  /** What the last tick's extract copied out for rendering. */
  public val snapshot: RenderSnapshot = RenderSnapshot(entityCapacity)

  /** Reads and writes this world's commands and state, the module's types included. */
  public val codec: WorldCodec = worldCodec {
    component(Quad::class, Quad.serializer())
    replicated(QuadReplicated)
    module.registerTypes(this)
    if (assets != null) registerAssetCommands()
  }

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

  private val assetSync: AssetSync? = assets?.let { AssetSync(it.database, it, history) }

  /** How long one step is, in seconds. */
  internal val stepSeconds = fixedStep.stepNanos / NANOS_PER_SECOND

  init {
    if (peer == PeerId.HOST) module.populate(this)
  }

  /** Runs one tick: the asset reloads, then every system, phase by phase, one fixed step long. */
  public fun step() {
    assetSync?.beforeTick(tick)
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

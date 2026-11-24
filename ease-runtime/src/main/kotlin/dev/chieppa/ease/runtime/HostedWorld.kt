package dev.chieppa.ease.runtime

import com.github.quillraven.fleks.World
import dev.chieppa.ease.commands.Command
import dev.chieppa.ease.commands.CommandHistory
import dev.chieppa.ease.commands.CommandOrigin
import dev.chieppa.ease.commands.WorldCodec
import dev.chieppa.ease.commands.worldCodec
import dev.chieppa.ease.ecs.DEFAULT_ENTITY_CAPACITY
import dev.chieppa.ease.ecs.Phase
import dev.chieppa.ease.ecs.phasedWorld
import dev.chieppa.ease.ecs.replication.ReplicationSettings
import dev.chieppa.ease.render.ExtractSystem
import dev.chieppa.ease.render.Quad
import dev.chieppa.ease.render.QuadReplicated
import dev.chieppa.ease.render.RenderSnapshot
import dev.chieppa.ease.runtime.loop.FixedStep

private const val NANOS_PER_SECOND = 1_000_000_000f

/**
 * One world a host runs: a Fleks world with [module]'s systems and the engine's [ExtractSystem],
 * the codec and command history every outside change goes through, the [snapshot] rendering reads,
 * and the current [tick]. Building it registers [module]'s types and lets it populate the world.
 * With an [input] script, each [step] first sends the commands the script has due.
 */
public class HostedWorld(
  module: GameModule,
  public val fixedStep: FixedStep,
  entityCapacity: Int = DEFAULT_ENTITY_CAPACITY,
  private val input: InputScript? = null,
) {
  /** What the last tick's extract copied out for rendering. */
  public val snapshot: RenderSnapshot = RenderSnapshot(entityCapacity)

  /** Reads and writes this world's commands and state, the module's types included. */
  public val codec: WorldCodec = worldCodec {
    component(Quad::class, Quad.serializer())
    replicated(QuadReplicated)
    module.registerTypes(this)
  }

  /**
   * The entities, components and systems, the codec's replicated types replicating. Change it from
   * outside only through [execute].
   */
  public val world: World =
    phasedWorld(entityCapacity, ReplicationSettings(serializers = codec.replicated)) {
      module.systems(this)
      add(Phase.EXTRACT, ExtractSystem(snapshot))
    }

  /** Every change made through [execute], with undo, redo and the command log. */
  public val history: CommandHistory = CommandHistory(world, codec)

  /** The tick the next [step] runs, which is also the number of steps run so far. */
  public var tick: Long = 0
    private set

  private val stepSeconds = fixedStep.stepNanos / NANOS_PER_SECOND

  init {
    module.populate(this)
  }

  /**
   * Runs one tick: the input script's commands, then every system, phase by phase, with one fixed
   * step as the delta.
   */
  public fun step() {
    input?.send(this)
    world.update(stepSeconds)
    tick++
  }

  /**
   * Applies [command] through the history, stamped with the current tick and [origin]. Throws
   * `CommandRejected`, changing nothing, when it cannot apply.
   */
  public fun execute(command: Command, origin: CommandOrigin = CommandOrigin.GAME) {
    history.execute(command, tick, origin)
  }
}

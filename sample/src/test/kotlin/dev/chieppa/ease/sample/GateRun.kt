package dev.chieppa.ease.sample

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import com.github.quillraven.fleks.World.Companion.family
import dev.chieppa.ease.ecs.replication.Authority
import dev.chieppa.ease.net.input.InputFrame
import dev.chieppa.ease.net.transport.LinkConditions
import dev.chieppa.ease.runtime.HostedWorld
import dev.chieppa.ease.runtime.LocalHost
import dev.chieppa.ease.runtime.loop.FixedStep
import dev.chieppa.ease.scene.node.NodeKey
import dev.chieppa.ease.scene.runtime.SceneNodes
import java.nio.file.Path

/**
 * The sample as the player runs it, a host and its local client with the courier script, over a
 * copy of the sample's content in [root] and a [BAD] link. Every [step] records the host's
 * replicated state and the input it took for the tick it ran, so a client's view can be matched
 * against the host's at any recorded tick and the log replayed with the same input.
 */
internal class GateRun(root: Path) {
  val fixedStep: FixedStep = FixedStep.perSecond()
  val content = SampleContent(root)
  val local = LocalHost(SampleGame(), fixedStep, CourierScript(), content.assets)
  val client = local.client
  private val serializers = local.host.codec.replicated
  private val state = ReplicatedState(serializers)
  private val history = HashMap<Long, Map<Long, EntityState>>()
  private val inputs = HashMap<Long, InputFrame>()
  val match = WorldMatch(serializers, ::hostAt)

  init {
    local.link.conditions = BAD
  }

  /** Runs [ticks] host ticks, recording each. */
  fun step(ticks: Int = 1) =
    repeat(ticks) {
      local.step()
      val ran = local.tick - 1
      history[ran] = state.of(local.host.world)
      val host = local.host
      host.inputs.of(LocalHost.LOCAL_PEER)?.let { taken ->
        inputs[ran] = InputFrame(local.settings.input).also { it.copyFrom(taken) }
      }
    }

  /** Steps until [done] holds, at most [limit] ticks; returns whether it came to hold. */
  fun stepUntil(limit: Int, done: () -> Boolean): Boolean {
    var left = limit
    while (!done() && left-- > 0) step()
    return done()
  }

  /** The host's replicated entities by network id after it ran [tick]. */
  fun hostAt(tick: Long): Map<Long, EntityState> =
    checkNotNull(history[tick]) { "Tick $tick of the host was not recorded" }

  /** Lets the link settle: its latency stays, but nothing is lost, duplicated or reordered. */
  fun settle() {
    local.link.conditions = SETTLED
  }

  /** The client's view now, matched once the host has run the client's tick. */
  fun mismatchesNow(): List<String> {
    val view = match.view(client)
    while (local.tick <= view.predicted) step()
    return match.mismatches(view)
  }

  /** The keys of every live node in [world], as text, sorted. */
  fun keys(world: World): List<String> {
    val index = world.system<SceneNodes>().index
    return index.keys.map { it.text }.sorted()
  }

  /** The courier the local client owns in [world]. */
  fun courier(world: World): Entity =
    with(world) {
      family { all(Courier, Authority) }.first { it[Authority].owner == LocalHost.LOCAL_PEER }
    }

  /** The live node [key] of [world]. */
  fun node(world: World, key: NodeKey): Entity = world.system<SceneNodes>().index[key]

  /** A world the host's log is replayed into, given the input the host took each tick. */
  fun replay(): HostedWorld {
    val live = local.host
    val replayed = HostedWorld(SampleGame(), fixedStep, assets = content.assets, replaying = true)
    fun stepReplayed() {
      val input = inputs[replayed.tick]
      if (input == null) replayed.inputs.remove(LocalHost.LOCAL_PEER)
      else replayed.inputs[LocalHost.LOCAL_PEER] = input
      replayed.step()
    }
    val log = live.history.log
    log.all.forEach { entry ->
      while (replayed.tick < entry.tick) stepReplayed()
      replayed.execute(log.commandOf(entry), entry.origin)
    }
    while (replayed.tick < live.tick) stepReplayed()
    return replayed
  }

  companion object {
    /** The link the gate runs over: latency, jitter, loss and duplication, on a seeded schedule. */
    val BAD = LinkConditions(latencyTicks = 4, jitterTicks = 3, loss = 0.15, duplication = 0.05)

    /** The same link once it settles: its latency alone. */
    val SETTLED = LinkConditions(latencyTicks = BAD.latencyTicks)
  }
}

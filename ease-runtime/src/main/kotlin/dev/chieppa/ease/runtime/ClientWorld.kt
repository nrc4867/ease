package dev.chieppa.ease.runtime

import dev.chieppa.ease.commands.Command
import dev.chieppa.ease.ecs.prediction.InterpolatedEntities
import dev.chieppa.ease.ecs.prediction.PredictedEntities
import dev.chieppa.ease.ecs.replication.replication
import dev.chieppa.ease.ecs.replication.stepPredicted
import dev.chieppa.ease.net.channel.Connection
import dev.chieppa.ease.net.input.InputFrame
import dev.chieppa.ease.net.input.InputRing
import dev.chieppa.ease.net.prediction.ClockSync
import dev.chieppa.ease.net.prediction.InterpolationClock
import dev.chieppa.ease.net.prediction.PredictionSettings
import dev.chieppa.ease.net.session.ClientSession
import dev.chieppa.ease.net.session.NetSettings
import dev.chieppa.ease.net.session.SnapshotListener
import dev.chieppa.ease.net.view.ReplicaContent

/**
 * A client's [world] joined to its host over [connection]: the host's entities arrive as spawns and
 * snapshots, and the client's [input] script and [inputFrame] go to the host through the [session].
 * This is the world a player sees and steers.
 *
 * Each tick runs at once from the client's own input, its predicted systems moving the entities it
 * owns ahead of the host; its clock keeps it far enough ahead that the input reaches the host in
 * time. When a snapshot's state for an owned entity differs from what was predicted for that tick,
 * the entity takes the host's state and the input since is replayed, the visible jump smoothed over
 * a few ticks. Everything it neither holds nor predicts is drawn a few ticks in the past, between
 * received states. A tick allocates nothing but the commands it sends.
 */
public class ClientWorld(
  public val world: HostedWorld,
  connection: Connection,
  settings: NetSettings,
  content: ReplicaContent = ReplicaContent.NONE,
  private val input: InputScript? = null,
  prediction: PredictionSettings = PredictionSettings(),
) {
  private val predicted = PredictedEntities(world.world, prediction.historyTicks)
  private val interpolated = InterpolatedEntities(world.world, prediction.interpolationStates)
  private val clock = ClockSync(prediction.clock)
  private val interpolation = InterpolationClock(prediction.interpolationDelayTicks)
  private val sent = InputRing(settings.input, prediction.historyTicks)
  private val replication = world.world.replication
  private var snapshotsThisReceive = 0
  private var observedSlackTick = -1L

  /** The client's side of the session with the host. */
  public val session: ClientSession =
    ClientSession(
      world.world,
      world.codec,
      connection,
      settings,
      content,
      SnapshotListener { tick ->
        interpolated.snapshotApplied(tick)
        snapshotsThisReceive++
      },
    )

  /**
   * The input the client sends with its next tick; whatever fills it (devices, a recording, the
   * input script) sets it before the tick.
   */
  public val inputFrame: InputFrame = InputFrame(settings.input)

  /** The client's tick the next step runs. */
  public val tick: Long
    get() = world.tick

  /** The client's step rate relative to the host's, which its clock steers. */
  public val clockRate: Double
    get() = clock.rate

  /** The host tick the entities drawn in the past are shown at, negative before any snapshot. */
  public val interpolationTime: Double
    get() = interpolation.time

  /** How many times a snapshot disagreed with the prediction and the input was replayed. */
  public var replays: Long = 0
    private set

  /** Sends [command] to the host, stamped with the client's [tick], for it to check and apply. */
  public fun send(command: Command) {
    session.sendCommand(world.tick, command)
  }

  /**
   * One host tick's worth: takes what the host sent, reconciling and replaying when it must, then
   * runs as many ticks of its own as its clock asks for, usually one.
   */
  internal fun step() {
    receive()
    repeat(clock.ticksThisStep()) { tickOnce() }
  }

  private fun receive() {
    predicted.beforeReceive()
    interpolated.beforeReceive()
    snapshotsThisReceive = 0
    session.receive()
    val replayFrom = predicted.afterReceive(session.latestSnapshotTick, snapshotsThisReceive > 0)
    if (replayFrom != PredictedEntities.NO_REPLAY) replay(replayFrom)
    interpolated.present(interpolation.time)
    followClock()
  }

  /** Replays the input of every tick after [from] up to the last one run, from the host's state. */
  private fun replay(from: Long) {
    if (!sent.reaches(from + 1)) {
      predicted.forget()
      return
    }
    replays++
    for (tick in from + 1 until world.tick) {
      world.inputs[world.peer] = sent.frameAt(tick)
      replication.stepPredicted()
      predicted.record(tick)
    }
    predicted.smooth(world.corrections)
  }

  /** Takes the host's newest report on the input's slack, jumping ahead when the clock says to. */
  private fun followClock() {
    val reported = session.inputSlackTick
    if (reported < 0 || reported == observedSlackTick) return
    observedSlackTick = reported
    val jump = clock.observe(session.inputSlack, reported, world.tick)
    if (jump > 0) world.skipTo(world.tick + jump)
  }

  /** One tick: the input script, this tick's input to the host, then the predicted step. */
  private fun tickOnce() {
    val tick = world.tick
    input?.send(this)
    session.sendInput(tick, inputFrame)
    sent.record(tick, inputFrame)
    world.inputs[world.peer] = inputFrame
    world.corrections.decay()
    interpolation.advance(session.latestSnapshotTick)
    interpolated.present(interpolation.time)
    world.step()
    predicted.record(tick)
  }
}

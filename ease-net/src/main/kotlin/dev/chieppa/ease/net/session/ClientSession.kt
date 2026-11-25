package dev.chieppa.ease.net.session

import com.github.quillraven.fleks.World
import dev.chieppa.ease.commands.Command
import dev.chieppa.ease.commands.WorldCodec
import dev.chieppa.ease.ecs.replication.BitWriter
import dev.chieppa.ease.ecs.replication.DecodeStatus
import dev.chieppa.ease.ecs.replication.replication
import dev.chieppa.ease.net.channel.ChannelKind
import dev.chieppa.ease.net.channel.Channels
import dev.chieppa.ease.net.channel.Connection
import dev.chieppa.ease.net.command.CommandFrames
import dev.chieppa.ease.net.input.InputFrame
import dev.chieppa.ease.net.input.InputHistory
import dev.chieppa.ease.net.input.InputPacket
import dev.chieppa.ease.net.snapshot.SnapshotReceiver
import dev.chieppa.ease.net.view.ReplicaContent
import dev.chieppa.ease.net.view.ViewReceiver
import dev.chieppa.ease.net.view.ViewRecord
import dev.chieppa.ease.net.view.ViewRefusal

/**
 * A client's side of a session over [world]. The host's spawns bring replicas of its entities into
 * the world under the host's network ids, held by the host, scene nodes built from the client's own
 * files through [content]; its despawns remove them, and its snapshots change them. The client
 * sends its input every tick on the unreliable channel with the last few ticks repeated, and
 * commands on the reliable ordered one; [listener] hears of each snapshot as it applies. Both ends
 * first exchange a join hello (protocol version, types hash and content digest); nothing else is
 * taken until the host's matches. Sending input and commands and applying snapshots and despawns
 * allocate nothing.
 */
public class ClientSession(
  world: World,
  private val codec: WorldCodec,
  private val connection: Connection,
  private val settings: NetSettings = NetSettings(),
  content: ReplicaContent = ReplicaContent.NONE,
  listener: SnapshotListener = SnapshotListener.NONE,
) {
  private val record = ViewRecord()
  private val views = ViewReceiver(world, connection.remote, content, record)
  private val snapshots = SnapshotReceiver(world.replication, record, listener)
  private val history = InputHistory(settings.input)
  private val packet = InputPacket(settings.input)
  private val commands = codec.commandWriter()
  private val outgoing = BitWriter(CommandFrames.MAX_BYTES)
  private val incoming = ByteArray(Channels.MAX_RELIABLE_BYTES)
  private var greeted = false

  init {
    JoinHello.send(connection, codec.typesHash, settings.content)
  }

  /**
   * Why the connection closed, whichever end closed it, or null while it is open: the client's own
   * reason, or the one the host sent with its close, such as a join refused for its content.
   */
  public val closeReason: CloseReason?
    get() = if (connection.isOpen) null else CloseReason.of(connection.closeCode)

  /** Why the client refused the spawn or despawn that closed the connection, or null. */
  public var viewRefusal: ViewRefusal? = null
    private set

  /** Why the client refused the snapshot that closed the connection, or null. */
  public var snapshotRefusal: DecodeStatus? = null
    private set

  /** Whether the connection is open. */
  public val isOpen: Boolean
    get() = connection.isOpen

  /** The tick of the newest snapshot applied, or -1 before the first. */
  public val latestSnapshotTick: Long
    get() = snapshots.latestTick

  /**
   * The last input tick the host had processed when it sent the newest applied snapshot, or -1:
   * where a predicting client replays its input from.
   */
  public val lastProcessedInputTick: Long
    get() = snapshots.lastInputTick

  /**
   * The input tick the host last reported this client's input slack on, or -1 before any report.
   */
  public val inputSlackTick: Long
    get() = snapshots.inputSlackTick

  /**
   * How many ticks before the host needed it the input of [inputSlackTick] arrived, negative when
   * it came late: what a client's clock steers by.
   */
  public val inputSlack: Long
    get() = snapshots.inputSlack

  /**
   * Takes the host's join hello, then applies every waiting spawn and despawn, then every waiting
   * snapshot newer than the last applied, and returns whether the connection is still open. A hello
   * that differs closes it with the reason, such as [CloseReason.CONTENT_MISMATCH]; a refused spawn
   * or despawn with [CloseReason.CONTENT_MISMATCH] or [CloseReason.MALFORMED_SPAWN] and
   * [viewRefusal]; a refused snapshot with [CloseReason.MALFORMED_SNAPSHOT] and [snapshotRefusal].
   * Before the hello, snapshots are dropped.
   */
  public fun receive(): Boolean {
    receiveReliable()
    receiveSnapshots()
    return connection.isOpen
  }

  /**
   * Records [frame] as the input of [tick] and sends it with the ticks before it, as many as the
   * window's redundancy, and the acknowledgement of the snapshots applied.
   */
  public fun sendInput(tick: Long, frame: InputFrame) {
    history.record(tick, frame)
    history.fill(packet, settings.window.redundancy)
    packet.ackTick = snapshots.latestTick
    packet.ackEarlier = snapshots.earlierApplied
    val length = packet.write(outgoing)
    connection.send(ChannelKind.UNRELIABLE, outgoing.buffer, 0, length)
  }

  /**
   * Sends [command], stamped with the client's [tick], for the host to check and apply. Throws
   * [IllegalArgumentException], sending nothing, for a command the host would refuse as malformed:
   * longer than `WorldCodec.MAX_COMMAND_BYTES`, nested too deep or with too long a collection.
   */
  public fun sendCommand(tick: Long, command: Command) {
    val length = CommandFrames.write(outgoing, commands, tick, command)
    connection.send(ChannelKind.RELIABLE_ORDERED, outgoing.buffer, 0, length)
  }

  private fun receiveReliable() {
    while (connection.isOpen) {
      val length = connection.receive(ChannelKind.RELIABLE_ORDERED, incoming)
      if (length == Channels.NO_MESSAGE) return
      if (greeted) applyView(length) else greet(length)
    }
  }

  private fun receiveSnapshots() {
    while (connection.isOpen) {
      val length = connection.receive(ChannelKind.UNRELIABLE, incoming)
      if (length == Channels.NO_MESSAGE) return
      val status = if (greeted) snapshots.receive(incoming, length) else DecodeStatus.OK
      if (status != DecodeStatus.OK) {
        snapshotRefusal = status
        close(CloseReason.MALFORMED_SNAPSHOT)
      }
    }
  }

  private fun applyView(length: Int) {
    val refusal = views.receive(incoming, length) ?: return
    viewRefusal = refusal
    close(
      if (refusal.contentMismatch) CloseReason.CONTENT_MISMATCH else CloseReason.MALFORMED_SPAWN
    )
  }

  /** Takes the host's join hello, closing the connection with the reason when it differs. */
  private fun greet(length: Int) {
    val refusal = JoinHello.refusal(incoming, length, codec.typesHash, settings.content)
    greeted = refusal == null
    if (refusal != null) close(refusal)
  }

  private fun close(reason: CloseReason) {
    connection.close(reason.code)
  }
}

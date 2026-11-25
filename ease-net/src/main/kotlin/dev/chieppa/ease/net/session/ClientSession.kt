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
import dev.chieppa.ease.net.view.ViewRefusal

/**
 * A client's side of a session over [world]. The host's spawns bring replicas of its entities into
 * the world under the host's network ids, held by the host, scene nodes built from the client's own
 * files through [content]; its despawns remove them, and its snapshots change them. The client
 * sends its input every tick on the unreliable channel with the last few ticks repeated, and
 * commands on the reliable ordered one. Both ends first exchange a hello with the hash of their
 * types; nothing else is taken until the host's matches. Sending input and commands and applying
 * snapshots and despawns allocate nothing.
 */
public class ClientSession(
  world: World,
  private val codec: WorldCodec,
  private val connection: Connection,
  private val settings: NetSettings = NetSettings(),
  content: ReplicaContent = ReplicaContent.NONE,
) {
  private val snapshots = SnapshotReceiver(world.replication)
  private val views = ViewReceiver(world, connection.remote, content)
  private val history = InputHistory(settings.input)
  private val packet = InputPacket(settings.input)
  private val commands = codec.commandWriter()
  private val outgoing = BitWriter(CommandFrames.MAX_BYTES)
  private val incoming = ByteArray(Channels.MAX_RELIABLE_BYTES)
  private var greeted = false

  init {
    Hello.send(codec, connection)
  }

  /** Why the client closed the connection, or null while it is open or closed by the host. */
  public var closeReason: CloseReason? = null
    private set

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
   * Takes the host's hello, then applies every waiting spawn and despawn, then every waiting
   * snapshot newer than the last applied, and returns whether the connection is still open. A hello
   * that differs closes it with [CloseReason.TYPES_MISMATCH]; a refused spawn or despawn with
   * [CloseReason.CONTENT_MISMATCH] or [CloseReason.MALFORMED_SPAWN] and [viewRefusal]; a refused
   * snapshot with [CloseReason.MALFORMED_SNAPSHOT] and [snapshotRefusal]. Before the hello,
   * snapshots are dropped.
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
      val status =
        if (greeted) snapshots.receive(incoming, length, views.applied) else DecodeStatus.OK
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

  /** Takes the host's hello, closing the connection when it differs. */
  private fun greet(length: Int) {
    greeted = Hello.matches(codec, incoming, length)
    if (!greeted) close(CloseReason.TYPES_MISMATCH)
  }

  private fun close(reason: CloseReason) {
    closeReason = reason
    connection.close()
  }
}

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

/**
 * A client's side of a session over [world], whose entities mirror the host's under the same
 * network ids: it applies the host's snapshots, sends its input every tick on the unreliable
 * channel with the last few ticks repeated, and sends commands on the reliable ordered one. Both
 * ends first exchange a hello with the hash of their types; snapshots apply only once the host's
 * matches. Sending and receiving allocate nothing.
 */
public class ClientSession(
  world: World,
  private val codec: WorldCodec,
  private val connection: Connection,
  private val settings: NetSettings = NetSettings(),
) {
  private val snapshots = SnapshotReceiver(world.replication)
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
   * Applies every waiting snapshot that is newer than the last applied, and returns
   * [DecodeStatus.OK] or the status that refused one, which closes the connection. Before the
   * host's hello has matched, snapshots are dropped; a hello that differs closes the connection
   * with [CloseReason.TYPES_MISMATCH] and returns [DecodeStatus.MALFORMED].
   */
  public fun receive(): DecodeStatus {
    if (!greeted && !greet()) return DecodeStatus.MALFORMED
    var status = DecodeStatus.OK
    while (status == DecodeStatus.OK && connection.isOpen) {
      val length = connection.receive(ChannelKind.UNRELIABLE, incoming)
      if (length == Channels.NO_MESSAGE) break
      if (greeted) status = snapshots.receive(incoming, length)
    }
    if (status != DecodeStatus.OK) {
      closeReason = CloseReason.MALFORMED_SNAPSHOT
      connection.close()
    }
    return status
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

  /** Takes the host's hello if it has come, and returns false when it differs. */
  private fun greet(): Boolean {
    val length = connection.receive(ChannelKind.RELIABLE_ORDERED, incoming)
    if (length == Channels.NO_MESSAGE) return true
    greeted = Hello.matches(codec, incoming, length)
    if (!greeted) {
      closeReason = CloseReason.TYPES_MISMATCH
      connection.close()
    }
    return greeted
  }
}

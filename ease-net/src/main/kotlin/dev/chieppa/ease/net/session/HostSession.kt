package dev.chieppa.ease.net.session

import com.github.quillraven.fleks.World
import dev.chieppa.ease.commands.WorldCodec
import dev.chieppa.ease.ecs.replication.DecodeStatus
import dev.chieppa.ease.net.channel.ChannelKind
import dev.chieppa.ease.net.channel.Channels
import dev.chieppa.ease.net.channel.Connection
import dev.chieppa.ease.net.command.CommandExecutor
import dev.chieppa.ease.net.command.CommandRefusals
import dev.chieppa.ease.net.snapshot.ChangeSink
import dev.chieppa.ease.net.snapshot.ChangeTracker

/**
 * The authority's side of a session over [world]: it takes each client's input and commands, and
 * sends each one delta snapshots of the entities it holds. Every entity is relevant to every peer.
 * Each tick the host calls [receive], updates the world, then calls [send]. Once its tables have
 * grown, a tick allocates nothing but the commands it decodes.
 */
public class HostSession(
  internal val world: World,
  internal val codec: WorldCodec,
  public val settings: NetSettings = NetSettings(),
  internal val executor: CommandExecutor = CommandExecutor { command, _, _ ->
    command.apply(world)
  },
  internal val refusals: CommandRefusals = CommandRefusals.IGNORE,
) {
  internal val tracker = ChangeTracker(world)
  private val connected = ArrayList<HostPeer>()
  private val buffer = ByteArray(Channels.MAX_RELIABLE_BYTES)
  private val fold = ChangeSink { slot, networkId, fields ->
    for (index in connected.indices) {
      val peer = connected[index]
      if (peer.isOpen) peer.snapshots.changed(slot, networkId, fields)
    }
  }

  /** Every client that connected, open or closed. */
  public val peers: List<HostPeer>
    get() = connected

  /**
   * Adds the client at the far end of [connection] and sends it the host's hello. Once the client's
   * own hello matches, it receives snapshots of the entities the host holds now, every field in the
   * first; entities and components added later are left out until spawns are replicated.
   */
  public fun connect(connection: Connection): HostPeer {
    val peer = HostPeer(connection, this)
    connected += peer
    return peer
  }

  /**
   * Takes every waiting input packet and command from every client, at the host's [tick], before
   * the world's update for that tick.
   */
  public fun receive(tick: Long) {
    for (index in connected.indices) {
      val peer = connected[index]
      receiveCommands(peer, tick)
      if (peer.greeted) receiveInputs(peer, tick)
    }
  }

  /** Sends every open client the snapshot of [tick], after the world's update for that tick. */
  public fun send(tick: Long) {
    tracker.update(fold)
    for (index in connected.indices) {
      val peer = connected[index]
      if (!peer.isOpen || !peer.greeted) continue
      val length = peer.snapshots.write(tick, peer.inputs.lastProcessedTick, buffer)
      peer.connection.send(ChannelKind.UNRELIABLE, buffer, 0, length)
    }
  }

  private fun receiveInputs(peer: HostPeer, tick: Long) {
    while (peer.isOpen) {
      val length = peer.connection.receive(ChannelKind.UNRELIABLE, buffer)
      if (length == Channels.NO_MESSAGE) return
      if (peer.packet.read(buffer, length) != DecodeStatus.OK) {
        peer.close(CloseReason.MALFORMED_INPUT)
        return
      }
      if (peer.inputs.offer(peer.packet, tick)) {
        peer.snapshots.acknowledge(peer.packet.ackTick, peer.packet.ackEarlier)
      }
    }
  }

  private fun receiveCommands(peer: HostPeer, tick: Long) {
    while (peer.isOpen) {
      val length = peer.connection.receive(ChannelKind.RELIABLE_ORDERED, buffer)
      if (length == Channels.NO_MESSAGE) return
      if (!peer.greeted) {
        greet(peer, length)
        continue
      }
      peer.commands.receive(buffer, length, tick)
      if (peer.commands.mustClose) {
        val reason =
          if (peer.commands.malformed) CloseReason.MALFORMED_COMMAND else CloseReason.RATE_LIMITED
        peer.close(reason)
      }
    }
  }

  /** Takes the first message from [peer] as its hello, closing the connection when it differs. */
  private fun greet(peer: HostPeer, length: Int) {
    if (Hello.matches(codec, buffer, length)) peer.greeted = true
    else peer.close(CloseReason.TYPES_MISMATCH)
  }
}

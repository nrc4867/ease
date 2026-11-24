package dev.chieppa.ease.net.session

import dev.chieppa.ease.ecs.replication.PeerId
import dev.chieppa.ease.ecs.replication.replication
import dev.chieppa.ease.net.channel.Connection
import dev.chieppa.ease.net.command.CommandIntake
import dev.chieppa.ease.net.input.InputBuffer
import dev.chieppa.ease.net.input.InputPacket
import dev.chieppa.ease.net.snapshot.PeerSnapshots

/**
 * One client as the host sees it: its connection, the input it sent, its snapshots and the checks
 * on its commands. Game systems read [inputs] each tick.
 */
public class HostPeer
internal constructor(
  internal val connection: Connection,
  session: HostSession,
) {
  /** The client's peer id. */
  public val peer: PeerId = connection.remote

  /** The client's input, by tick. */
  public val inputs: InputBuffer = InputBuffer(session.settings.input, session.settings.window)

  /** Why the host closed this peer's connection, or null while it is open or closed by the peer. */
  public var closeReason: CloseReason? = null
    private set

  /** Whether the connection is open. */
  public val isOpen: Boolean
    get() = connection.isOpen

  /** Whether the client's hello matched the host's types; until then nothing else is taken. */
  public var greeted: Boolean = false
    internal set

  internal val snapshots = PeerSnapshots(session.world.replication, session.tracker)
  internal val packet = InputPacket(session.settings.input)
  internal val commands =
    CommandIntake(
      session.world,
      session.codec,
      peer,
      session.settings.rateLimits,
      session.executor,
      session.refusals,
    )

  init {
    Hello.send(session.codec, connection)
  }

  internal fun close(reason: CloseReason) {
    if (closeReason == null && connection.isOpen) closeReason = reason
    connection.close()
  }
}

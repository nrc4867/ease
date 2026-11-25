package dev.chieppa.ease.net.session

import dev.chieppa.ease.ecs.replication.PeerId
import dev.chieppa.ease.ecs.replication.replication
import dev.chieppa.ease.net.channel.Connection
import dev.chieppa.ease.net.command.CommandIntake
import dev.chieppa.ease.net.input.InputBuffer
import dev.chieppa.ease.net.input.InputPacket
import dev.chieppa.ease.net.input.InputSlack
import dev.chieppa.ease.net.relevancy.Viewer
import dev.chieppa.ease.net.snapshot.PeerPriority
import dev.chieppa.ease.net.snapshot.PeerSnapshots
import dev.chieppa.ease.net.view.PeerView

/**
 * One client as the host sees it: its connection, the input it sent, where it looks from, the
 * entities in its view, its snapshots and the checks on its commands. Game systems read [inputs]
 * each tick and set the [viewer], usually by focusing it on the client's own character.
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

  /**
   * Why the connection closed, whichever end closed it, or null while it is open: the host's own
   * reason, or the one the client sent with its close.
   */
  public val closeReason: CloseReason?
    get() = if (connection.isOpen) null else CloseReason.of(connection.closeCode)

  /** Whether the connection is open. */
  public val isOpen: Boolean
    get() = connection.isOpen

  /**
   * Whether the client's join hello matched the host's protocol, types and content; until then
   * nothing else is taken.
   */
  public var greeted: Boolean = false
    internal set

  /** Where the client looks at the world from, which relevancy and priority read. */
  public val viewer: Viewer = Viewer(peer)

  private val priority =
    PeerPriority(session.world, viewer, session.motion, session.viewSettings.priority)
  internal val snapshots = PeerSnapshots(session.world.replication, session.tracker, priority)
  internal val view =
    PeerView(
      session.world,
      session.tracker.entities,
      viewer,
      session.viewSettings,
      snapshots,
      session.spawns,
    )
  internal val packet = InputPacket(session.settings.input)
  internal val slack = InputSlack()
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
    JoinHello.send(connection, session.codec.typesHash, session.settings.content)
  }

  internal fun close(reason: CloseReason) {
    connection.close(reason.code)
  }
}

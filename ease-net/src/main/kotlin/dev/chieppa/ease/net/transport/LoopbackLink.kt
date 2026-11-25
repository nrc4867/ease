package dev.chieppa.ease.net.transport

import dev.chieppa.ease.ecs.replication.PeerId
import dev.chieppa.ease.net.channel.ChannelKind
import dev.chieppa.ease.net.channel.Channels
import dev.chieppa.ease.net.channel.Connection
import java.util.SplittableRandom

/**
 * A host and one client joined in one process: what [hostEnd] sends, [clientEnd] receives, and the
 * other way round, each direction under the simulated network of [conditions], which may change at
 * any tick. Its transport moves it on a tick at a time.
 */
public class LoopbackLink
internal constructor(
  hostPeer: PeerId,
  clientPeer: PeerId,
  conditions: LinkConditions,
  toClientSeed: Long,
  toHostSeed: Long,
) {
  private val toClient = SimulatedPath(SplittableRandom(toClientSeed))
  private val toHost = SimulatedPath(SplittableRandom(toHostSeed))
  private var code = Channels.STILL_OPEN

  /** The simulated network, the same in both directions. */
  public var conditions: LinkConditions = conditions
    set(value) {
      field = value
      toClient.conditions = value
      toHost.conditions = value
    }

  init {
    this.conditions = conditions
  }

  /** The host's end, whose remote peer is the client. */
  public val hostEnd: Connection = End(clientPeer, outgoing = toClient, incoming = toHost)

  /** The client's end, whose remote peer is the host. */
  public val clientEnd: Connection = End(hostPeer, outgoing = toHost, incoming = toClient)

  /** Whether neither end has closed the link. */
  public val isOpen: Boolean
    get() = code == Channels.STILL_OPEN

  internal fun advance() {
    if (!isOpen) return
    toClient.advance()
    toHost.advance()
  }

  internal fun closeWith(closeCode: Int) {
    require(closeCode in Channels.CLOSED_PLAINLY..Channels.MAX_CLOSE_CODE) {
      "A close code runs from ${Channels.CLOSED_PLAINLY} to ${Channels.MAX_CLOSE_CODE}, " +
        "not $closeCode"
    }
    if (!isOpen) return
    code = closeCode
    toClient.clear()
    toHost.clear()
  }

  private inner class End(
    override val remote: PeerId,
    private val outgoing: SimulatedPath,
    private val incoming: SimulatedPath,
  ) : Connection {
    override val isOpen: Boolean
      get() = this@LoopbackLink.isOpen

    override val closeCode: Int
      get() = code

    override fun send(kind: ChannelKind, bytes: ByteArray, offset: Int, length: Int) {
      require(length <= kind.maxBytes) { "$kind carries ${kind.maxBytes} bytes, not $length" }
      if (isOpen) outgoing.send(kind, bytes, offset, length)
    }

    override fun receive(kind: ChannelKind, target: ByteArray): Int =
      if (isOpen) incoming.receive(kind, target) else Channels.NO_MESSAGE

    override fun close(code: Int) {
      closeWith(code)
    }
  }
}

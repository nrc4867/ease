package dev.chieppa.ease.net.channel

import dev.chieppa.ease.ecs.replication.PeerId

/**
 * Two connected ends in one process: what [hostEnd] sends, [clientEnd] receives, and the other way
 * round. Every channel delivers every message at once and in order, so it stands in for a perfect
 * network; tests that need loss or reordering wrap an end.
 */
public class LocalLink(hostPeer: PeerId = PeerId.HOST, clientPeer: PeerId) {
  private val toClient = Array(ChannelKind.entries.size) { MessageQueue(INITIAL_BYTES) }
  private val toHost = Array(ChannelKind.entries.size) { MessageQueue(INITIAL_BYTES) }
  private var open = true

  /** The host's end, whose remote peer is the client. */
  public val hostEnd: Connection = End(clientPeer, outgoing = toClient, incoming = toHost)

  /** The client's end, whose remote peer is the host. */
  public val clientEnd: Connection = End(hostPeer, outgoing = toHost, incoming = toClient)

  private inner class End(
    override val remote: PeerId,
    private val outgoing: Array<MessageQueue>,
    private val incoming: Array<MessageQueue>,
  ) : Connection {
    override val isOpen: Boolean
      get() = open

    override fun send(kind: ChannelKind, bytes: ByteArray, offset: Int, length: Int) {
      require(length <= kind.maxBytes) { "$kind carries ${kind.maxBytes} bytes, not $length" }
      if (open) outgoing[kind.ordinal].push(bytes, offset, length)
    }

    override fun receive(kind: ChannelKind, target: ByteArray): Int =
      if (open) incoming[kind.ordinal].pop(target) else Channels.NO_MESSAGE

    override fun close() {
      open = false
      toClient.forEach { it.clear() }
      toHost.forEach { it.clear() }
    }
  }

  private companion object {
    const val INITIAL_BYTES = 16 * 1024
  }
}

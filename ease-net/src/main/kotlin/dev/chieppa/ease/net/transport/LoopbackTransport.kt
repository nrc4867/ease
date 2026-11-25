package dev.chieppa.ease.net.transport

import dev.chieppa.ease.ecs.replication.PeerId
import dev.chieppa.ease.net.channel.Connection

/**
 * The [Transport] for a host and its clients in one process: single-player, editor multi-client
 * play and network tests. Each [connect] makes a [LoopbackLink] under its own simulated network;
 * [update] is the links' clock, a tick at a time, and drops links either end closed, so delays are
 * counted in ticks and tests advance them by hand. Every random draw comes from a schedule seeded
 * by [seed] and the client's peer id, so a run repeats exactly. On a perfect network, the default,
 * a message arrives as it is sent.
 */
public class LoopbackTransport(
  private val seed: Long = 0L,
  private val hostPeer: PeerId = PeerId.HOST,
) : Transport {
  private val links = ArrayList<LoopbackLink>()
  private val unaccepted = ArrayDeque<Connection>()

  /** The ticks [update] has moved on since the transport was made. */
  public var tick: Long = 0L
    private set

  /**
   * Opens a link from the client [peer] to the host under [conditions]. The host takes its end from
   * [accept]; the client talks through [LoopbackLink.clientEnd].
   */
  public fun connect(
    peer: PeerId,
    conditions: LinkConditions = LinkConditions.PERFECT,
  ): LoopbackLink {
    val streams = seed xor (peer.value.toLong() * SEED_SPREAD)
    val link = LoopbackLink(hostPeer, peer, conditions, streams, streams xor TO_HOST)
    links += link
    unaccepted.addLast(link.hostEnd)
    return link
  }

  override fun accept(): Connection? = unaccepted.removeFirstOrNull()

  override fun update() {
    tick++
    var index = links.size - 1
    while (index >= 0) {
      val link = links[index]
      if (link.isOpen) link.advance() else links.removeAt(index)
      index--
    }
  }

  override fun close() {
    for (index in links.indices) links[index].hostEnd.close()
    unaccepted.clear()
  }

  private companion object {
    /** Spreads peers' seeds apart (the 64-bit golden ratio). */
    const val SEED_SPREAD = -0x61c8864680b583ebL

    /** Tells the direction to the host apart from the one to the client. */
    const val TO_HOST = 0x5DEECE66DL
  }
}

package dev.chieppa.ease.net

import dev.chieppa.ease.net.channel.ChannelKind
import dev.chieppa.ease.net.channel.Channels
import dev.chieppa.ease.net.channel.Connection

/**
 * Wraps [inner] so the test decides when messages arrive: while a channel is [hold]ing, what comes
 * in on it waits, and [release] lets the oldest waiting ones through, in order. A fake for a lossy
 * transport whose reliable channel lags and whose unreliable one reorders.
 */
internal class LaggingConnection(private val inner: Connection) : Connection by inner {
  private val holding = mutableSetOf<ChannelKind>()
  private val waiting = ChannelKind.entries.associateWith { ArrayDeque<ByteArray>() }
  private val released = ChannelKind.entries.associateWith { ArrayDeque<ByteArray>() }

  /** Holds back what arrives on [kind] from now on. */
  fun hold(kind: ChannelKind) {
    holding += kind
  }

  /** Lets [count] of the oldest messages waiting on [kind] through, or all of them. */
  fun release(kind: ChannelKind, count: Int = Int.MAX_VALUE) {
    pull(kind)
    val queue = waiting.getValue(kind)
    repeat(minOf(count, queue.size)) { released.getValue(kind).addLast(queue.removeFirst()) }
  }

  /** Stops holding [kind] and lets everything waiting on it through. */
  fun resume(kind: ChannelKind) {
    release(kind)
    holding -= kind
  }

  override fun receive(kind: ChannelKind, target: ByteArray): Int {
    if (kind in holding) pull(kind)
    val next = released.getValue(kind).removeFirstOrNull()
    return when {
      next != null -> next.copyInto(target).let { next.size }
      kind in holding -> Channels.NO_MESSAGE
      else -> inner.receive(kind, target)
    }
  }

  private fun pull(kind: ChannelKind) {
    val buffer = ByteArray(Channels.MAX_RELIABLE_BYTES)
    while (true) {
      val length = inner.receive(kind, buffer)
      if (length == Channels.NO_MESSAGE) return
      waiting.getValue(kind).addLast(buffer.copyOf(length))
    }
  }
}

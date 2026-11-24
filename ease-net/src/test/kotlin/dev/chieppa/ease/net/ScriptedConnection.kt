package dev.chieppa.ease.net

import dev.chieppa.ease.net.channel.ChannelKind
import dev.chieppa.ease.net.channel.Channels
import dev.chieppa.ease.net.channel.Connection

/** What happens to one unreliable message a [ScriptedConnection] receives. */
internal enum class Fate {
  DELIVER,
  DROP,

  /** Held back, then delivered right after the next message that is delivered. */
  DELAY,
}

/**
 * Wraps [inner] so the unreliable messages it receives meet the fate [script] gives the nth one: a
 * fake for a lossy, reordering network. It also counts the bytes of the last message delivered.
 */
internal class ScriptedConnection(
  private val inner: Connection,
  private val script: (Int) -> Fate,
) : Connection by inner {
  private var received = 0
  private val held = ByteArray(Channels.MAX_UNRELIABLE_BYTES)
  private var heldLength = Channels.NO_MESSAGE
  private var releaseHeld = false

  /** The length of the last unreliable message delivered. */
  var lastLength = 0
    private set

  override fun receive(kind: ChannelKind, target: ByteArray): Int =
    when {
      kind != ChannelKind.UNRELIABLE -> inner.receive(kind, target)
      releaseHeld -> release(target)
      else -> next(target)
    }

  private fun next(target: ByteArray): Int {
    var result = UNSETTLED
    while (result == UNSETTLED) {
      val length = inner.receive(ChannelKind.UNRELIABLE, target)
      result = if (length == Channels.NO_MESSAGE) length else settle(target, length)
    }
    return result
  }

  private fun settle(target: ByteArray, length: Int): Int =
    when (script(received++)) {
      Fate.DROP -> UNSETTLED
      Fate.DELAY -> if (heldLength == Channels.NO_MESSAGE) hold(target, length) else deliver(length)
      Fate.DELIVER -> deliver(length)
    }

  private fun hold(target: ByteArray, length: Int): Int {
    target.copyInto(held, 0, 0, length)
    heldLength = length
    return Channels.NO_MESSAGE
  }

  private fun deliver(length: Int): Int {
    releaseHeld = heldLength != Channels.NO_MESSAGE
    lastLength = length
    return length
  }

  private fun release(target: ByteArray): Int {
    val length = heldLength
    held.copyInto(target, 0, 0, length)
    heldLength = Channels.NO_MESSAGE
    releaseHeld = false
    lastLength = length
    return length
  }

  private companion object {
    /** A message that met its fate without being delivered yet. */
    const val UNSETTLED = -2
  }
}

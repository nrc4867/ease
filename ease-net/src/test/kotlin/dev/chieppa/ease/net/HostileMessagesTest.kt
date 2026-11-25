package dev.chieppa.ease.net

import dev.chieppa.ease.net.channel.ChannelKind
import dev.chieppa.ease.net.channel.Channels
import dev.chieppa.ease.net.channel.LocalLink
import dev.chieppa.ease.net.session.ClientSession
import dev.chieppa.ease.net.session.CloseReason
import dev.chieppa.ease.net.session.Hello
import dev.chieppa.ease.net.session.HostSession
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Every message a peer receives is hostile: random bytes, every truncation of a real message and
 * bit flips in one are taken or refused, never thrown past the session, and a refused message
 * changes nothing in the world. The generator is seeded, so every run feeds the same bytes.
 */
class HostileMessagesTest {
  private val session = Session(entities = 4, owned = 2)
  private val random = Random(SEED)
  private val scratch = ByteArray(Channels.MAX_RELIABLE_BYTES)

  init {
    // Exchange hellos, so what follows on each channel is the messages under test.
    session.step()
  }

  /** The real messages the client sends on [kind], captured off the host's end. */
  private fun captured(kind: ChannelKind, send: () -> Unit): ByteArray {
    send()
    val length = session.link.hostEnd.receive(kind, scratch)
    return scratch.copyOf(length)
  }

  /**
   * Feeds [bytes] to a fresh connection of the session's host on [kind]; the host's world stays.
   */
  private fun feedHost(kind: ChannelKind, bytes: ByteArray) {
    val link = LocalLink(clientPeer = Session.CLIENT)
    val host = HostSession(session.host, session.codec, session.settings)
    val peer = host.connect(link.hostEnd)
    Hello.send(session.codec, link.clientEnd)
    link.clientEnd.send(kind, bytes, 0, bytes.size)
    host.receive(HOST_TICK)
    if (!peer.isOpen) {
      assertTrue(peer.closeReason in HOST_CLOSES, peer.closeReason.toString())
    }
  }

  /**
   * Feeds [bytes] to a fresh client session over the session's client world, after the spawns the
   * session's client applied, and returns whether it took them.
   */
  private fun feedClient(bytes: ByteArray): Boolean {
    val link = LocalLink(clientPeer = Session.CLIENT)
    val client = ClientSession(session.client, session.codec, link.clientEnd, session.settings)
    Hello.send(session.codec, link.hostEnd)
    link.hostEnd.send(ChannelKind.UNRELIABLE, bytes, 0, bytes.size)
    val open = client.receive()
    if (!open) assertEquals(CloseReason.MALFORMED_SNAPSHOT, client.closeReason)
    return open
  }

  private fun variants(real: ByteArray): List<ByteArray> {
    val cuts = (0 until real.size).map { real.copyOf(it) }
    val flips =
      List(FLIPS) {
        val bytes = real.copyOf()
        repeat(1 + random.nextInt(MAX_FLIPS)) {
          val index = random.nextInt(bytes.size)
          bytes[index] = (bytes[index].toInt() xor (1 shl random.nextInt(Byte.SIZE_BITS))).toByte()
        }
        bytes
      }
    val noise = List(NOISE) { random.nextBytes(random.nextInt(MAX_NOISE)) }
    return cuts + flips + noise
  }

  @Test
  fun `hostile input packets never throw and never change the world`() {
    val real =
      captured(ChannelKind.UNRELIABLE) { session.clientSession.sendInput(HOST_TICK, session.input) }
    val before = session.states()

    variants(real).forEach { feedHost(ChannelKind.UNRELIABLE, it) }

    assertEquals(before, session.states())
  }

  @Test
  fun `hostile command messages never throw`() {
    val real =
      captured(ChannelKind.RELIABLE_ORDERED) {
        session.clientSession.sendCommand(HOST_TICK, Push(session.idOf(0), 0.5f))
      }

    variants(real).forEach { feedHost(ChannelKind.RELIABLE_ORDERED, it) }
  }

  @Test
  fun `every truncation of a command message is refused`() {
    val real =
      captured(ChannelKind.RELIABLE_ORDERED) {
        session.clientSession.sendCommand(HOST_TICK, Push(session.idOf(0), 0.5f))
      }
    val before = session.states()

    for (cut in 0 until real.size) {
      val link = LocalLink(clientPeer = Session.CLIENT)
      val host = HostSession(session.host, session.codec, session.settings)
      val peer = host.connect(link.hostEnd)
      Hello.send(session.codec, link.clientEnd)
      link.clientEnd.send(ChannelKind.RELIABLE_ORDERED, real, 0, cut)
      host.receive(HOST_TICK)
      assertEquals(CloseReason.MALFORMED_COMMAND, peer.closeReason, "cut at $cut")
    }
    assertEquals(before, session.states())
  }

  @Test
  fun `hostile snapshots never throw, and a refused one changes nothing`() {
    session.moveOnHost(0, 2f)
    session.hostSession.send(HOST_TICK)
    val real = scratch.copyOf(session.link.clientEnd.receive(ChannelKind.UNRELIABLE, scratch))
    // A fresh client has applied no spawns, so the header's spawn count goes to 0 for the variants.
    real[SEQUENCE_BYTE] = 0

    variants(real).forEach { bytes ->
      val before = session.states().second
      if (!feedClient(bytes)) assertEquals(before, session.states().second)
    }
  }

  private companion object {
    const val SEED = 90_210L

    /** The spawn count's byte in a snapshot header whose tick and input tick take one byte each. */
    const val SEQUENCE_BYTE = 2
    const val HOST_TICK = 40L
    const val FLIPS = 1_000
    const val MAX_FLIPS = 4
    const val NOISE = 1_000
    const val MAX_NOISE = 300
    val HOST_CLOSES =
      setOf(CloseReason.MALFORMED_INPUT, CloseReason.MALFORMED_COMMAND, CloseReason.RATE_LIMITED)
  }
}

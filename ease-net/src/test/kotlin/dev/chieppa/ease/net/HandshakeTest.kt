package dev.chieppa.ease.net

import dev.chieppa.ease.commands.worldCodec
import dev.chieppa.ease.net.channel.ChannelKind
import dev.chieppa.ease.net.channel.LocalLink
import dev.chieppa.ease.net.session.ClientSession
import dev.chieppa.ease.net.session.CloseReason
import dev.chieppa.ease.net.session.HostSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Host and client first exchange a hash of the types their codecs number on the wire; peers that
 * registered different types are refused before either reads the other's snapshots or commands.
 */
class HandshakeTest {
  private val session = Session(entities = 1)
  private val link = LocalLink(clientPeer = Session.CLIENT)
  private val host = HostSession(session.host, session.codec, session.settings)
  private val peer = host.connect(link.hostEnd)

  @Test
  fun `peers with the same types proceed`() {
    session.step()
    session.step()

    assertTrue(session.peer.greeted)
    assertTrue(session.clientSession.isOpen)
    assertTrue(session.clientSession.latestSnapshotTick >= 0)
  }

  @Test
  fun `the host refuses a client whose types differ`() {
    ClientSession(session.client, worldCodec(), link.clientEnd, session.settings)
    host.receive(0)

    assertFalse(peer.isOpen)
    assertEquals(CloseReason.TYPES_MISMATCH, peer.closeReason)
  }

  @Test
  fun `a client refuses a host whose types differ`() {
    val client = ClientSession(session.client, worldCodec(), link.clientEnd, session.settings)
    client.receive()

    assertFalse(client.isOpen)
    assertEquals(CloseReason.TYPES_MISMATCH, client.closeReason)
  }

  @Test
  fun `a first message that is not a hello is refused`() {
    val bytes = byteArrayOf(1, 2, 3)
    link.clientEnd.send(ChannelKind.RELIABLE_ORDERED, bytes, 0, bytes.size)
    host.receive(0)

    assertEquals(CloseReason.TYPES_MISMATCH, peer.closeReason)
  }
}

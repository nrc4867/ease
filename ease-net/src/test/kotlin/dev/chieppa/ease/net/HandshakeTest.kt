package dev.chieppa.ease.net

import com.github.quillraven.fleks.Entity
import dev.chieppa.ease.commands.worldCodec
import dev.chieppa.ease.ecs.replication.entityOf
import dev.chieppa.ease.net.channel.ChannelKind
import dev.chieppa.ease.net.join.ContentManifest
import dev.chieppa.ease.net.join.ContentPart
import dev.chieppa.ease.net.session.ClientSession
import dev.chieppa.ease.net.session.CloseReason
import dev.chieppa.ease.net.session.HostSession
import dev.chieppa.ease.net.session.JoinHello
import dev.chieppa.ease.net.session.NetSettings
import dev.chieppa.ease.net.transport.LoopbackTransport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Host and client first exchange a join hello: the protocol version, a hash of the types their
 * codecs number on the wire, and the digest of the content they build from. A peer that differs in
 * any is refused at join with a typed reason that both ends read, before either takes the other's
 * snapshots or commands; a matching client gets the host's entities as spawns, then deltas.
 */
class HandshakeTest {
  private val shipped = NetSettings(content = ContentManifest.of(mods = listOf(PACK)))
  private val session = Session(entities = 1, settings = shipped)
  private val link = LoopbackTransport().connect(Session.CLIENT)
  private val host = HostSession(session.host, session.codec, session.settings)
  private val peer = host.connect(link.hostEnd)

  private fun join(client: ClientSession) {
    host.receive(0)
    client.receive()
  }

  @Test
  fun `a matching client gets a full view, then deltas`() {
    session.step()
    assertTrue(session.peer.greeted)
    assertNotEquals(Entity.NONE, session.mirror(0))

    session.moveOnHost(0, 3f)
    session.step()

    assertTrue(session.clientSession.isOpen)
    assertEquals(3f to 3f, session.xs(0))
  }

  @Test
  fun `a client with different content is refused at join, and both ends know why`() {
    val other = NetSettings(content = ContentManifest.of(mods = listOf(ContentPart("pack", "2"))))
    val client = ClientSession(session.client, session.codec, link.clientEnd, other)
    join(client)

    assertFalse(peer.isOpen)
    assertEquals(CloseReason.CONTENT_MISMATCH, peer.closeReason)
    assertEquals(CloseReason.CONTENT_MISMATCH, client.closeReason)
    assertEquals(Entity.NONE, session.client.entityOf(session.idOf(0)))
  }

  @Test
  fun `the host refuses a client whose types differ`() {
    val client = ClientSession(session.client, worldCodec(), link.clientEnd, shipped)
    join(client)

    assertEquals(CloseReason.TYPES_MISMATCH, peer.closeReason)
    assertEquals(CloseReason.TYPES_MISMATCH, client.closeReason)
  }

  @Test
  fun `a client refuses a host whose content differs`() {
    val stranger = LoopbackTransport().connect(Session.CLIENT)
    val client = ClientSession(session.client, session.codec, stranger.clientEnd, shipped)
    JoinHello.send(stranger.hostEnd, session.codec.typesHash, ContentManifest.EMPTY)
    client.receive()

    assertFalse(client.isOpen)
    assertEquals(CloseReason.CONTENT_MISMATCH, client.closeReason)
  }

  @Test
  fun `a client speaking another protocol version is refused`() {
    val hello =
      JoinHello.write(session.codec.typesHash, shipped.content, JoinHello.PROTOCOL_VERSION + 1)
    link.clientEnd.send(ChannelKind.RELIABLE_ORDERED, hello, 0, hello.size)
    host.receive(0)

    assertEquals(CloseReason.PROTOCOL_MISMATCH, peer.closeReason)
  }

  @Test
  fun `a first message that is not a hello is refused`() {
    val bytes = byteArrayOf(1, 2, 3)
    link.clientEnd.send(ChannelKind.RELIABLE_ORDERED, bytes, 0, bytes.size)
    host.receive(0)

    assertEquals(CloseReason.MALFORMED_HELLO, peer.closeReason)
  }

  @Test
  fun `the content digest ignores the order content is listed in`() {
    val parts = listOf(PACK, ContentPart("other", "9"))

    val forward = ContentManifest.of(mods = parts).digest
    val backward = ContentManifest.of(mods = parts.reversed()).digest

    assertTrue(forward.contentEquals(backward))
    assertFalse(forward.contentEquals(ContentManifest.EMPTY.digest))
  }

  @Test
  fun `fields that run together differently give different digests`() {
    val spaced = ContentManifest.of(mods = listOf(ContentPart("pack b", "1"))).digest
    val shifted = ContentManifest.of(mods = listOf(ContentPart("pack", "b 1"))).digest

    assertFalse(spaced.contentEquals(shifted))
  }

  private companion object {
    val PACK = ContentPart("pack", "1")
  }
}

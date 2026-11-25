package dev.chieppa.ease.net

import com.github.quillraven.fleks.Entity
import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.ecs.replication.DecodeStatus
import dev.chieppa.ease.ecs.replication.PeerId
import dev.chieppa.ease.ecs.replication.entityOf
import dev.chieppa.ease.ecs.replication.networkIdOf
import dev.chieppa.ease.ecs.replication.spawnEntity
import dev.chieppa.ease.net.channel.ChannelKind
import dev.chieppa.ease.net.channel.Channels
import dev.chieppa.ease.net.channel.LocalLink
import dev.chieppa.ease.net.session.ClientSession
import dev.chieppa.ease.net.session.CloseReason
import dev.chieppa.ease.net.session.Hello
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Snapshots keep applying while spawns and despawns lag on the reliable channel and snapshots come
 * out of order: one sent before a despawn the client already applied passes over the removed
 * entity, and one naming an id the client was never told about is still refused.
 */
class LaggingChannelsTest {
  private var lagging: LaggingConnection? = null
  private val session =
    Session(entities = 3) { LaggingConnection(it).also { end -> lagging = end } }
  private val channels: LaggingConnection
    get() = checkNotNull(lagging)

  init {
    session.step()
  }

  @Test
  fun `replicas keep moving while spawns and despawns lag`() {
    channels.hold(ChannelKind.RELIABLE_ORDERED)
    val late = session.host.spawnEntity { it += LocalTransform() }
    val gone = session.idOf(2)
    session.host -= session.held[2]
    for (step in 1..LAG) {
      session.moveOnHost(0, step.toFloat())
      session.step()
      assertEquals(step.toFloat() to step.toFloat(), session.xs(0), "while lagging, step $step")
    }

    channels.resume(ChannelKind.RELIABLE_ORDERED)
    repeat(SETTLE) { session.step() }

    assertTrue(session.clientSession.isOpen)
    assertNotEquals(Entity.NONE, session.client.entityOf(session.host.networkIdOf(late)))
    assertEquals(Entity.NONE, session.client.entityOf(gone))
  }

  @Test
  fun `a snapshot sent before a despawn passes over the removed entity`() {
    channels.hold(ChannelKind.UNRELIABLE)
    session.moveOnHost(0, MOVED)
    session.moveOnHost(1, MOVED)
    session.step()
    session.host -= session.held[1]
    session.step()

    channels.release(ChannelKind.UNRELIABLE, count = 1)
    session.clientSession.receive()

    assertTrue(session.clientSession.isOpen, session.clientSession.snapshotRefusal.toString())
    assertEquals(MOVED to MOVED, session.xs(0))
    channels.resume(ChannelKind.UNRELIABLE)
    repeat(SETTLE) { session.step() }
    assertTrue(session.clientSession.isOpen)
  }

  @Test
  fun `a snapshot naming an id the client was never told about is refused`() {
    session.moveOnHost(0, MOVED)
    session.hostSession.send(session.tick)
    val scratch = ByteArray(Channels.MAX_RELIABLE_BYTES)
    val real = scratch.copyOf(channels.receive(ChannelKind.UNRELIABLE, scratch))
    real[SEQUENCE_BYTE] = 0
    val link = LocalLink(clientPeer = PeerId(2))
    val client = ClientSession(session.lateClient(PeerId(2)).first, session.codec, link.clientEnd)
    Hello.send(session.codec, link.hostEnd)
    link.hostEnd.send(ChannelKind.UNRELIABLE, real, 0, real.size)

    client.receive()

    assertEquals(CloseReason.MALFORMED_SNAPSHOT, client.closeReason)
    assertEquals(DecodeStatus.UNKNOWN_ENTITY, client.snapshotRefusal)
  }

  private companion object {
    const val LAG = 5
    const val SETTLE = 3
    const val MOVED = 6f

    /** The spawn count's byte in a header whose tick and input tick take one byte each. */
    const val SEQUENCE_BYTE = 2
  }
}

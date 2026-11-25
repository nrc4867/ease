package dev.chieppa.ease.net

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import dev.chieppa.ease.commands.worldCodec
import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.ecs.phasedWorld
import dev.chieppa.ease.ecs.replication.PeerId
import dev.chieppa.ease.ecs.replication.ReplicatedFields
import dev.chieppa.ease.ecs.replication.ReplicatedSchema
import dev.chieppa.ease.ecs.replication.ReplicationSettings
import dev.chieppa.ease.ecs.replication.entityOf
import dev.chieppa.ease.ecs.replication.networkIdOf
import dev.chieppa.ease.ecs.replication.replicatedSchema
import dev.chieppa.ease.ecs.replication.replication
import dev.chieppa.ease.ecs.replication.spawnEntity
import dev.chieppa.ease.net.channel.LocalLink
import dev.chieppa.ease.net.session.ClientSession
import dev.chieppa.ease.net.session.HostSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * An entity whose changes are too large for any snapshot on its own is left out, and the entities
 * after it still reach the client every tick; a debug build throws, naming it. Its spawn, a
 * reliable message, still fits.
 */
class OversizedEntityTest {
  private val schemas =
    List(SCHEMAS) { index ->
      replicatedSchema("test.Wide$index") {
        repeat(ReplicatedFields.MAX_FIELDS) { float("f$it") }
      }
    }
  private val codec = worldCodec {
    schemas.forEach { replicated(it.serializer) }
  }

  private fun world(peer: PeerId, debug: Boolean): World =
    phasedWorld(replication = ReplicationSettings(peer, codec.replicated, debug)) {}

  private inner class Worlds(debug: Boolean) {
    val host = world(PeerId.HOST, debug)
    val client = world(Session.CLIENT, debug = false)
    val wide: Entity = host.spawnEntity { entity -> schemas.forEach { entity += wideOf(it) } }
    val small: Entity = host.spawnEntity { it += LocalTransform() }

    val link = LocalLink(clientPeer = Session.CLIENT)
    val hostSession = HostSession(host, codec)
    val peer = hostSession.connect(link.hostEnd)
    val clientSession = ClientSession(client, codec, link.clientEnd)

    fun step(tick: Long) {
      hostSession.receive(tick)
      host.replication.write(small, LocalTransform) { x = tick.toFloat() }
      schemas.forEach { schema ->
        host.replication.write(wide, schema.type) {
          repeat(ReplicatedFields.MAX_FIELDS) { setFloat(it, tick.toFloat()) }
        }
      }
      hostSession.send(tick)
      clientSession.receive()
    }

    fun clientX(): Float =
      with(client) { client.entityOf(host.networkIdOf(small))[LocalTransform].x }
  }

  private fun wideOf(schema: ReplicatedSchema) =
    schema.component().also { component ->
      repeat(ReplicatedFields.MAX_FIELDS) { component.setFloat(it, 1f) }
    }

  @Test
  fun `the entities after one too large for a snapshot still arrive every tick`() {
    val pair = Worlds(debug = false)
    for (tick in 0L until TICKS) {
      pair.step(tick)
      assertEquals(tick.toFloat(), pair.clientX())
    }

    assertTrue(pair.clientSession.isOpen)
  }

  @Test
  fun `a debug build throws naming the entity too large for a snapshot`() {
    val pair = Worlds(debug = true)
    pair.step(0)

    // It waits behind the small entity until what it built up puts it first, alone in a snapshot.
    val thrown = assertFailsWith<IllegalStateException> { for (tick in 1L..TICKS) pair.step(tick) }

    val message = thrown.message.orEmpty()
    assertTrue("${pair.host.networkIdOf(pair.wide)}" in message, message)
  }

  private companion object {
    /** Ten components of 32 full floats: more than one snapshot holds. */
    const val SCHEMAS = 10
    const val TICKS = 10L
  }
}

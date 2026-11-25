package dev.chieppa.ease.net

import com.github.quillraven.fleks.Entity
import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.ecs.replication.Authority
import dev.chieppa.ease.ecs.replication.PeerId
import dev.chieppa.ease.ecs.replication.entityOf
import dev.chieppa.ease.ecs.replication.networkIdOf
import dev.chieppa.ease.ecs.replication.replication
import dev.chieppa.ease.ecs.replication.spawnEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * What the host changes about its entities during a session reaches the client through spawns and
 * despawns: new entities, removed ones, components added, and owners changed. The client's copies
 * carry the host's network ids and are held by the host.
 */
class LateEntitiesTest {
  private val session = Session(entities = 2)
  private val bare = session.host.spawnEntity()

  init {
    session.step()
  }

  @Test
  fun `the client's copies carry the host's ids and are held by the host`() {
    for (index in session.held.indices) {
      val mirror = session.mirror(index)
      assertNotEquals(Entity.NONE, mirror)
      assertEquals(PeerId.HOST, with(session.client) { mirror[Authority].holder })
      assertFalse(session.client.replication.hasAuthority(mirror))
    }
    assertNotEquals(
      Entity.NONE,
      session.client.entityOf(session.host.networkIdOf(bare)),
    )
  }

  @Test
  fun `an entity spawned during the session reaches the client with its state`() {
    val late = session.host.spawnEntity { it += LocalTransform().apply { x = MOVED } }
    session.step()

    val mirror = session.client.entityOf(session.host.networkIdOf(late))
    assertEquals(MOVED, with(session.client) { mirror[LocalTransform].x })
    assertTrue(session.clientSession.isOpen)
  }

  @Test
  fun `an entity removed on the host is removed on the client`() {
    val id = session.idOf(1)
    session.host -= session.held[1]
    session.step()

    assertEquals(Entity.NONE, session.client.entityOf(id))
    assertTrue(session.clientSession.isOpen)
  }

  @Test
  fun `a component added to an entity the client holds reaches it`() {
    with(session.host) { bare.configure { it += LocalTransform().apply { x = MOVED } } }
    session.step()
    session.host.replication.write(bare, LocalTransform) { x = MOVED * 2 }
    session.step()

    val mirror = session.client.entityOf(session.host.networkIdOf(bare))
    assertEquals(MOVED * 2, with(session.client) { mirror[LocalTransform].x })
  }

  @Test
  fun `a new owner reaches the client`() {
    val other = PeerId(2)
    with(session.host) { session.held[0][Authority].owner = other }
    session.step()

    assertEquals(other, with(session.client) { session.mirror(0)[Authority].owner })
  }

  @Test
  fun `the client keeps receiving changes after all of it`() {
    session.host.spawnEntity { it += LocalTransform() }
    session.host -= session.held[1]
    session.moveOnHost(0, MOVED)
    repeat(SETTLE) { session.step() }

    assertTrue(session.clientSession.isOpen)
    assertEquals(MOVED to MOVED, session.xs(0))
  }

  private companion object {
    const val MOVED = 4f
    const val SETTLE = 3
  }
}

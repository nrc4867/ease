package dev.chieppa.ease.net

import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.ecs.replication.spawnEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Until spawns replicate, a client is sent only the entities and components it held when it
 * connected: what the host adds later is left out, so the client stays connected and keeps
 * receiving the rest.
 */
class UnknownEntitiesTest {
  private val session = Session(entities = 2, bare = 1)

  private fun assertStillReceiving() {
    session.moveOnHost(0, MOVED)
    repeat(SETTLE) { session.step() }

    assertTrue(session.clientSession.isOpen)
    assertNull(session.clientSession.closeReason)
    assertEquals(MOVED to MOVED, session.xs(0))
  }

  @Test
  fun `an entity spawned during the session is left out of snapshots`() {
    session.step()
    session.host.spawnEntity { it += LocalTransform() }

    assertStillReceiving()
  }

  @Test
  fun `a component added to an entity the client holds is left out of snapshots`() {
    session.step()
    with(session.host) { session.bareHeld[0].configure { it += LocalTransform() } }

    assertStillReceiving()
  }

  private companion object {
    const val MOVED = 4f
    const val SETTLE = 3
  }
}

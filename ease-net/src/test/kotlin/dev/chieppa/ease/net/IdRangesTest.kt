package dev.chieppa.ease.net

import com.github.quillraven.fleks.Entity
import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.ecs.replication.NetworkId
import dev.chieppa.ease.ecs.replication.networkIdOf
import dev.chieppa.ease.ecs.replication.replication
import dev.chieppa.ease.ecs.replication.spawnEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * A client's own entities, made before and during a session, never clash with the host's: the host
 * issues the ids that go on the wire, and a client issues its own from the local range.
 */
class IdRangesTest {
  @Test
  fun `entities a client makes itself never clash with the host's spawns`() {
    val session = Session(entities = 3)
    val before = List(LOCAL) { session.client.spawnEntity { entity -> entity += LocalTransform() } }
    session.step()
    val during = List(LOCAL) { session.client.spawnEntity { entity -> entity += LocalTransform() } }
    session.host.spawnEntity { it += LocalTransform() }
    session.moveOnHost(0, MOVED)
    repeat(SETTLE) { session.step() }

    assertTrue(session.clientSession.isOpen)
    assertNull(session.clientSession.viewRefusal)
    for (index in 0 until 3) assertNotEquals(Entity.NONE, session.mirror(index))
    assertEquals(MOVED to MOVED, session.xs(0))
    for (entity in before + during) {
      val id = session.client.networkIdOf(entity)
      assertTrue(id.value >= NetworkId.LOCAL_START, "$id")
      assertTrue(session.client.replication.hasAuthority(entity))
    }
  }

  private companion object {
    const val LOCAL = 4
    const val MOVED = 2f
    const val SETTLE = 3
  }
}

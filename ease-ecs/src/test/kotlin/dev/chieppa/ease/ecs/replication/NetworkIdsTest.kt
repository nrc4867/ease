package dev.chieppa.ease.ecs.replication

import com.github.quillraven.fleks.Entity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class NetworkIdsTest {
  private val world = ReplicationWorlds.world()

  @Test
  fun `every new entity gets a network id and authority held by the world's peer`() {
    val entity = world.entity {}

    val id = world.networkIdOf(entity)
    val authority = with(world) { entity[Authority] }

    assertEquals(NetworkId(1), id)
    assertEquals(PeerId.HOST, authority.holder)
    assertEquals(PeerId.HOST, authority.owner)
    assertTrue(world.replication.hasAuthority(entity))
    assertEquals(entity, world.entityOf(id))
  }

  @Test
  fun `a network id is never issued again, though Fleks reuses the entity id`() {
    val first = world.entity {}
    val firstId = world.networkIdOf(first)
    world -= first

    val second = world.entity {}

    assertEquals(first.id, second.id)
    assertNotEquals(firstId, world.networkIdOf(second))
    assertEquals(Entity.NONE, world.entityOf(firstId))
  }

  @Test
  fun `an entity created with an id keeps it, and later ids are issued past it`() {
    val received = world.entity { it += NetworkIdentity(NetworkId(RECEIVED)) }
    val next = world.entity {}

    assertEquals(NetworkId(RECEIVED), world.networkIdOf(received))
    assertEquals(NetworkId(RECEIVED + 1), world.networkIdOf(next))
  }

  @Test
  fun `an entity held by another peer is a replica here`() {
    val replica = ReplicationWorlds.lanternEntity(world, holder = PeerId(3))

    assertFalse(world.replication.hasAuthority(replica))
  }

  private companion object {
    const val RECEIVED = 40L
  }
}

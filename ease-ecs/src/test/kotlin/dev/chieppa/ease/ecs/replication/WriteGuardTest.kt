package dev.chieppa.ease.ecs.replication

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Writing replicated components: through a write scope, with authority, checked in debug. */
class WriteGuardTest {
  private val world = ReplicationWorlds.world()
  private val replication = world.replication

  @Test
  fun `a write with authority changes the component and marks it changed`() {
    val entity = ReplicationWorlds.lanternEntity(world)

    val written = replication.write(entity, Lantern) { fuel = 5f }

    assertTrue(written)
    assertEquals(5f, ReplicationWorlds.lanternOf(world, entity).fuel)
    assertTrue(replication.isDirty(entity, Lantern))
    replication.clearDirty()
    assertFalse(replication.isDirty(entity, Lantern))
  }

  @Test
  fun `writing without authority throws in a debug build, naming entity and component`() {
    val replica = ReplicationWorlds.lanternEntity(world, holder = PeerId(2))
    val id = world.networkIdOf(replica)

    val thrown =
      assertFailsWith<AuthorityViolation> { replication.write(replica, Lantern) { fuel = 1f } }

    val message = thrown.message.orEmpty()
    assertTrue("Entity $id" in message, message)
    assertTrue("dev.chieppa.ease.ecs.replication.Lantern" in message, message)
    assertEquals(0f, ReplicationWorlds.lanternOf(world, replica).fuel)
  }

  @Test
  fun `writing without authority in a release build changes nothing`() {
    val release = ReplicationWorlds.world(debug = false)
    val replica = ReplicationWorlds.lanternEntity(release, holder = PeerId(2))

    val written = release.replication.write(replica, Lantern) { fuel = 1f }

    assertFalse(written)
    assertEquals(0f, ReplicationWorlds.lanternOf(release, replica).fuel)
  }

  @Test
  fun `a direct change without authority throws at the end of the tick`() {
    val replica = ReplicationWorlds.lanternEntity(world, holder = PeerId(2))
    world.update(DELTA)
    ReplicationWorlds.lanternOf(world, replica).charges = 9

    val thrown = assertFailsWith<AuthorityViolation> { world.update(DELTA) }

    val message = thrown.message.orEmpty()
    assertTrue("without authority" in message, message)
    assertTrue("Entity ${world.networkIdOf(replica)}" in message, message)
  }

  @Test
  fun `a direct change with authority throws too, since it would not replicate`() {
    val entity = ReplicationWorlds.lanternEntity(world)
    world.update(DELTA)
    ReplicationWorlds.lanternOf(world, entity).lit = true

    val thrown = assertFailsWith<AuthorityViolation> { world.update(DELTA) }

    assertTrue("outside Replication.write" in thrown.message.orEmpty(), thrown.message)
  }

  @Test
  fun `changes through the write scope pass the end-of-tick check`() {
    val entity = ReplicationWorlds.lanternEntity(world)
    world.update(DELTA)

    replication.write(entity, Lantern) { brightness = 0.5f }
    world.update(DELTA)

    assertEquals(0.5f, ReplicationWorlds.lanternOf(world, entity).brightness)
  }

  @Test
  fun `a new component on a reused entity id is not taken for a change`() {
    val first = ReplicationWorlds.lanternEntity(world)
    world.update(DELTA)
    world -= first
    val second = world.entity { it += Lantern().also { lantern -> lantern.fuel = 8f } }

    world.update(DELTA)

    assertEquals(first.id, second.id)
  }

  @Test
  fun `a release build does not check at the end of the tick`() {
    val release = ReplicationWorlds.world(debug = false)
    val replica = ReplicationWorlds.lanternEntity(release, holder = PeerId(2))
    release.update(DELTA)
    ReplicationWorlds.lanternOf(release, replica).charges = 9

    release.update(DELTA)

    assertEquals(9, ReplicationWorlds.lanternOf(release, replica).charges)
  }

  private companion object {
    const val DELTA = 1f / 60f
  }
}

package dev.chieppa.ease.commands

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.Snapshot
import dev.chieppa.ease.commands.builtin.Destroy
import dev.chieppa.ease.commands.builtin.Restore
import dev.chieppa.ease.commands.builtin.SetTransform
import dev.chieppa.ease.commands.builtin.Spawn
import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.ecs.phasedWorld
import dev.chieppa.ease.ecs.replication.Authority
import dev.chieppa.ease.ecs.replication.NetworkId
import dev.chieppa.ease.ecs.replication.PeerId
import dev.chieppa.ease.ecs.replication.ReplicationSettings
import dev.chieppa.ease.ecs.replication.entityOf
import dev.chieppa.ease.ecs.replication.networkIdOf
import dev.chieppa.ease.math.Quat
import dev.chieppa.ease.math.Transform
import dev.chieppa.ease.math.Vec3
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/** Scenario tests: commands through the real history, outcomes checked as full world state. */
class CommandHistoryTest {
  private val codec = worldCodec()
  private val world = freshWorld()
  private val history = CommandHistory(world, codec)
  private val moved =
    Transform(Vec3(1f, 2f, 3f), Quat.fromAxisAngle(Vec3(0f, 1f, 0f), 0.5f), Vec3.ONE)

  private fun state(): String = codec.worldToJson(world)

  private fun freshWorld() =
    phasedWorld(replication = ReplicationSettings(serializers = codec.replicated)) {}

  /** Spawns an entity with a transform and returns its network id, the newest in the world. */
  private fun spawn(): NetworkId {
    history.execute(Spawn(listOf(LocalTransform.of(Transform.IDENTITY))), 0, CommandOrigin.USER)
    return world.asEntityBag().map { world.networkIdOf(it) }.maxBy { it.value }
  }

  private fun components(authority: Authority): List<Component<out Any>> =
    listOf(LocalTransform(), authority)

  private fun isLive(id: NetworkId): Boolean = world.entityOf(id) != Entity.NONE

  @Test
  fun `move, undo and redo return the world to exactly the same state`() {
    val entity = spawn()
    world.entity { it += LocalTransform() }
    val before = state()

    history.execute(SetTransform(entity, moved), tick = 1, origin = CommandOrigin.USER)
    val after = state()
    assertNotEquals(before, after)

    assertTrue(history.undo(tick = 2, origin = CommandOrigin.USER))
    assertEquals(before, state())
    assertTrue(history.redo(tick = 3, origin = CommandOrigin.USER))
    assertEquals(after, state())
  }

  @Test
  fun `undoing a spawn and redoing it brings back the same entity`() {
    val empty = state()
    val entity = spawn()
    val spawned = state()

    history.undo(1, CommandOrigin.USER)
    assertEquals(empty, state())
    history.redo(2, CommandOrigin.USER)

    assertEquals(spawned, state())
    assertTrue(isLive(entity))
  }

  @Test
  fun `undoing a destroy restores the entity under its id with its components`() {
    val entity = spawn()
    history.execute(SetTransform(entity, moved), 1, CommandOrigin.USER)
    val alive = state()

    history.execute(Destroy(entity), 2, CommandOrigin.USER)
    assertFalse(isLive(entity))
    history.undo(3, CommandOrigin.USER)

    assertEquals(alive, state())
  }

  @Test
  fun `a rejected command changes nothing and is not logged`() {
    val entity = spawn()
    history.execute(Destroy(entity), 1, CommandOrigin.USER)
    val before = state()
    val logged = history.log.all.size

    assertFailsWith<CommandRejected> {
      history.execute(SetTransform(entity, moved), 2, CommandOrigin.AGENT)
    }

    assertEquals(before, state())
    assertEquals(logged, history.log.all.size)
    assertEquals(CommandOrigin.USER, history.nextUndoOrigin)
  }

  @Test
  fun `a new command clears redo`() {
    val entity = spawn()
    history.execute(SetTransform(entity, moved), 1, CommandOrigin.USER)
    history.undo(2, CommandOrigin.USER)

    history.execute(SetTransform(entity, Transform.IDENTITY), 3, CommandOrigin.USER)

    assertFalse(history.canRedo)
    assertFalse(history.redo(4, CommandOrigin.USER))
  }

  @Test
  fun `the log records tick and origin of every change, undo and redo included`() {
    val entity = spawn()
    history.execute(SetTransform(entity, moved), 5, CommandOrigin.AGENT)
    history.undo(6, CommandOrigin.USER)
    history.redo(7, CommandOrigin.USER)

    val entries = history.log.all
    assertEquals(listOf(0L, 5L, 6L, 7L), entries.map { it.tick })
    assertEquals(
      listOf(CommandOrigin.USER, CommandOrigin.AGENT, CommandOrigin.USER, CommandOrigin.USER),
      entries.map { it.origin },
    )
    assertEquals(CommandOrigin.AGENT, history.nextUndoOrigin)
  }

  @Test
  fun `replaying the log onto a fresh world reproduces the state`() {
    val entity = spawn()
    history.execute(SetTransform(entity, moved), 1, CommandOrigin.USER)
    history.execute(Spawn(listOf(LocalTransform.of(moved))), 2, CommandOrigin.USER)
    history.undo(3, CommandOrigin.USER)
    history.execute(Destroy(entity), 4, CommandOrigin.USER)
    history.undo(5, CommandOrigin.USER)

    val fresh = freshWorld()
    history.log.replayOnto(fresh)

    assertEquals(state(), codec.worldToJson(fresh))
  }

  @Test
  fun `a logged command names its entity by network id, so a reused Fleks id is never hit`() {
    val first = spawn()
    val second = spawn()
    history.execute(Destroy(first), 1, CommandOrigin.USER)
    val third = spawn()
    val reused = world.entityOf(third)

    history.execute(SetTransform(second, moved), 2, CommandOrigin.USER)
    history.undo(3, CommandOrigin.USER)
    history.undo(4, CommandOrigin.USER)
    history.undo(5, CommandOrigin.USER)

    assertTrue(isLive(first))
    assertFalse(isLive(third))
    assertEquals(Transform.IDENTITY, with(world) { world.entityOf(second)[LocalTransform].get() })
    assertNotEquals(Entity.NONE, reused)
  }

  @Test
  fun `a command on an entity another peer holds is rejected`() {
    val entity = spawn()
    with(world) { world.entityOf(entity)[Authority].holder = PeerId(2) }

    assertFailsWith<CommandRejected> {
      history.execute(SetTransform(entity, moved), 1, CommandOrigin.NETWORK)
    }
  }

  @Test
  fun `a restore under an id at or past the limit is rejected and later spawns still work`() {
    val hostile =
      Restore(NetworkId(Long.MAX_VALUE), Snapshot(listOf(LocalTransform()), emptyList()))
    val atLimit = Restore(NetworkId(NetworkId.LIMIT), Snapshot(emptyList(), emptyList()))

    assertFailsWith<CommandRejected> { history.execute(hostile, 1, CommandOrigin.NETWORK) }
    assertFailsWith<CommandRejected> { history.execute(atLimit, 1, CommandOrigin.NETWORK) }

    assertEquals(NetworkId(1), spawn())
  }

  @Test
  fun `a spawn or restore that hands authority to another peer is rejected`() {
    val foreign = Authority(holder = PeerId(2), owner = PeerId(2))
    val owned = Authority(holder = PeerId.HOST, owner = PeerId(2))

    assertFailsWith<CommandRejected> {
      history.execute(Spawn(components(foreign)), 1, CommandOrigin.NETWORK)
    }
    assertFailsWith<CommandRejected> {
      val snapshot = Snapshot(components(foreign), emptyList())
      history.execute(Restore(NetworkId(9), snapshot), 1, CommandOrigin.NETWORK)
    }
    history.execute(Spawn(components(owned)), 1, CommandOrigin.NETWORK)
    assertEquals(1, history.log.all.size)
  }

  @Test
  fun `an undo that can no longer apply is dropped, so the one under it is reachable`() {
    val first = spawn()
    val second = spawn()
    history.execute(SetTransform(first, moved), 1, CommandOrigin.USER)
    history.execute(SetTransform(second, moved), 2, CommandOrigin.USER)
    with(world) { world.entityOf(second)[Authority].holder = PeerId(2) }

    assertFailsWith<CommandRejected> { history.undo(3, CommandOrigin.USER) }
    assertTrue(history.undo(4, CommandOrigin.USER))

    assertEquals(Transform.IDENTITY, with(world) { world.entityOf(first)[LocalTransform].get() })
    assertEquals(moved, with(world) { world.entityOf(second)[LocalTransform].get() })
  }

  @Test
  fun `a redo that can no longer apply is dropped too`() {
    val entity = spawn()
    history.execute(SetTransform(entity, moved), 1, CommandOrigin.USER)
    history.undo(2, CommandOrigin.USER)
    with(world) { world.entityOf(entity)[Authority].holder = PeerId(2) }

    assertFailsWith<CommandRejected> { history.redo(3, CommandOrigin.USER) }

    assertFalse(history.canRedo)
  }

  @Test
  fun `changes the user did not ask for apply and are logged but are never undone`() {
    val entity = spawn()
    history.execute(SetTransform(entity, moved), 1, CommandOrigin.USER)
    val edited = state()
    val reload = SetTransform(entity, Transform.IDENTITY)

    for (origin in CommandOrigin.entries.filterNot { it.undoable }) {
      history.execute(reload, 2, origin)
      val logged = history.log.all.last()
      assertEquals(origin, logged.origin)
    }

    assertNotEquals(edited, state())
    assertEquals(CommandOrigin.USER, history.nextUndoOrigin)
    assertTrue(history.undo(3, CommandOrigin.USER))
    assertEquals(Transform.IDENTITY, with(world) { world.entityOf(entity)[LocalTransform].get() })
  }

  @Test
  fun `a change the user did not ask for leaves redo in place`() {
    val entity = spawn()
    history.execute(SetTransform(entity, moved), 1, CommandOrigin.USER)
    history.undo(2, CommandOrigin.USER)

    history.execute(Spawn(listOf(LocalTransform())), 3, CommandOrigin.GAME)

    assertTrue(history.redo(4, CommandOrigin.USER))
    assertEquals(moved, with(world) { world.entityOf(entity)[LocalTransform].get() })
  }

  @Test
  fun `only the user and agents acting for them can undo`() {
    assertEquals(
      setOf(CommandOrigin.USER, CommandOrigin.AGENT),
      CommandOrigin.entries.filter { it.undoable }.toSet(),
    )
  }
}

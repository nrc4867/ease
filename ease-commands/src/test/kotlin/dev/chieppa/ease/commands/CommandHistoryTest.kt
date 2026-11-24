package dev.chieppa.ease.commands

import com.github.quillraven.fleks.Entity
import dev.chieppa.ease.commands.builtin.Destroy
import dev.chieppa.ease.commands.builtin.SetTransform
import dev.chieppa.ease.commands.builtin.Spawn
import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.ecs.phasedWorld
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
  private val world = phasedWorld {}
  private val history = CommandHistory(world, codec)
  private val moved =
    Transform(Vec3(1f, 2f, 3f), Quat.fromAxisAngle(Vec3(0f, 1f, 0f), 0.5f), Vec3.ONE)

  private fun state(): String = codec.worldToJson(world)

  private fun spawn(): Entity {
    history.execute(Spawn(listOf(LocalTransform.of(Transform.IDENTITY))), 0, CommandOrigin.GAME)
    return world.asEntityBag().first()
  }

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
    assertTrue(entity in world)
  }

  @Test
  fun `undoing a destroy restores the entity under its id with its components`() {
    val entity = spawn()
    history.execute(SetTransform(entity, moved), 1, CommandOrigin.USER)
    val alive = state()

    history.execute(Destroy(entity), 2, CommandOrigin.USER)
    assertFalse(entity in world)
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
      listOf(CommandOrigin.GAME, CommandOrigin.AGENT, CommandOrigin.USER, CommandOrigin.USER),
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

    val fresh = phasedWorld {}
    history.log.replayOnto(fresh)

    assertEquals(state(), codec.worldToJson(fresh))
  }
}

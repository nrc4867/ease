package dev.chieppa.ease.scene.commands

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.wildcardSnapshotOf
import dev.chieppa.ease.commands.CommandHistory
import dev.chieppa.ease.commands.CommandOrigin
import dev.chieppa.ease.commands.CommandRejected
import dev.chieppa.ease.commands.builtin.Restore
import dev.chieppa.ease.commands.builtin.Spawn
import dev.chieppa.ease.commands.permissionRefusal
import dev.chieppa.ease.commands.worldCodec
import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.ecs.Phase
import dev.chieppa.ease.ecs.phasedWorld
import dev.chieppa.ease.ecs.replication.NetworkId
import dev.chieppa.ease.ecs.replication.PeerId
import dev.chieppa.ease.ecs.replication.ReplicationSettings
import dev.chieppa.ease.scene.Beacon
import dev.chieppa.ease.scene.Scenes
import dev.chieppa.ease.scene.node.LocalId
import dev.chieppa.ease.scene.node.NodeKey
import dev.chieppa.ease.scene.node.NodeName
import dev.chieppa.ease.scene.runtime.SceneNodes
import dev.chieppa.ease.scene.slots.NodeRef
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/** Scenario tests: node commands through the real history, with undo and redo. */
class NodeCommandsTest {
  private val codec = worldCodec {
    registerSceneCommands()
    component(Beacon::class, Beacon.serializer())
  }
  private val world =
    phasedWorld(replication = ReplicationSettings(serializers = codec.replicated)) {
      add(Phase.INPUT, SceneNodes(Scenes.types))
    }
  private val history = CommandHistory(world, codec)
  private val index = world.system<SceneNodes>().index
  private val lamp = NodeKey.of(LocalId(1))

  private fun state(): String = codec.worldToJson(world)

  private fun spawnLamp() {
    val components = listOf(lamp, NodeName("Lamp"), LocalTransform())
    history.execute(SpawnNode(wildcardSnapshotOf(components, emptyList())), 0, CommandOrigin.USER)
  }

  @Test
  fun `a spawned node is indexed by its key and undo removes it`() {
    val empty = state()
    spawnLamp()

    assertNotEquals(Entity.NONE, index[lamp])
    history.undo(1, CommandOrigin.USER)
    assertEquals(Entity.NONE, index[lamp])
    assertEquals(empty, state())
  }

  @Test
  fun `an update sets, adds and removes components, and undo restores them exactly`() {
    spawnLamp()
    val before = state()
    val changes = listOf(NodeName("Bright lamp"), Beacon(strength = 2f, target = NodeRef(lamp)))

    history.execute(
      UpdateNode(lamp, wildcardSnapshotOf(changes, emptyList()), listOf("ease.LocalTransform")),
      1,
      CommandOrigin.USER,
    )

    with(world) {
      assertEquals("Bright lamp", index[lamp][NodeName].name)
      assertEquals(lamp, index[lamp][Beacon].target?.key)
      assertEquals(null, index[lamp].getOrNull(LocalTransform))
    }
    history.undo(2, CommandOrigin.USER)
    assertEquals(before, state())
  }

  @Test
  fun `destroying a node and undoing it brings it back under its key`() {
    spawnLamp()

    history.execute(DestroyNode(lamp), 1, CommandOrigin.USER)
    assertEquals(Entity.NONE, index[lamp])
    history.undo(2, CommandOrigin.USER)

    assertEquals("Lamp", with(world) { index[lamp][NodeName].name })
  }

  @Test
  fun `a second node with a live key, or a missing node, is rejected`() {
    spawnLamp()

    assertFailsWith<CommandRejected> { spawnLamp() }
    assertFailsWith<CommandRejected> {
      history.execute(DestroyNode(NodeKey.of(LocalId(2))), 1, CommandOrigin.USER)
    }
  }

  @Test
  fun `only the world's own peer may send node commands, and validate changes nothing`() {
    spawnLamp()
    val before = state()
    val commands =
      listOf(
        DestroyNode(lamp),
        UpdateNode(lamp, wildcardSnapshotOf(listOf(NodeName("Dim")), emptyList()), emptyList()),
        SpawnNode(wildcardSnapshotOf(listOf(NodeKey.of(LocalId(2))), emptyList())),
      )

    commands.forEach { command ->
      assertNotNull(permissionRefusal(world, command, PeerId(1)))
      assertNull(permissionRefusal(world, command, PeerId.HOST))
      assertNull(command.validate(world))
    }
    assertNotNull(DestroyNode(NodeKey.of(LocalId(9))).validate(world))
    assertNotNull(
      UpdateNode(lamp, wildcardSnapshotOf(emptyList(), emptyList()), listOf("x.Y")).validate(world)
    )
    assertEquals(before, state())
  }

  @Test
  fun `a generic spawn or restore carrying a live node key is refused before anything is built`() {
    spawnLamp()
    val before = state()
    val entities = world.numEntities
    val copy: List<Component<out Any>> = listOf(NodeKey.of(LocalId(1)), NodeName("Copy"))

    assertFailsWith<CommandRejected> {
      history.execute(Spawn(copy), 1, CommandOrigin.USER)
    }
    assertFailsWith<CommandRejected> {
      history.execute(
        Restore(NetworkId(500), wildcardSnapshotOf(copy, emptyList())),
        1,
        CommandOrigin.USER,
      )
    }

    assertEquals(entities, world.numEntities)
    assertEquals(before, state())
    assertEquals("Lamp", with(world) { index[lamp][NodeName].name })
  }

  @Test
  fun `node commands read back from JSON as they were written`() {
    val command = DestroyNode(NodeKey.of(listOf(LocalId(4)), LocalId(1)))

    val json = codec.commandToJson(command)

    assertEquals(
      """{"type":"ease.DestroyNode","node":{"path":"0000000000000004/0000000000000001"}}""",
      json,
    )
    assertEquals(json, codec.commandToJson(codec.commandFromJson(json)))
  }
}

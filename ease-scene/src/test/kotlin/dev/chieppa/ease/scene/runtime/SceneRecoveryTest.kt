package dev.chieppa.ease.scene.runtime

import dev.chieppa.ease.assets.Guid
import dev.chieppa.ease.assets.meta.AssetMeta
import dev.chieppa.ease.assets.meta.MetaFiles
import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.ecs.replication.replication
import dev.chieppa.ease.platform.files.FileChangeKind
import dev.chieppa.ease.scene.Scenes
import dev.chieppa.ease.scene.Scenes.document
import dev.chieppa.ease.scene.Scenes.node
import dev.chieppa.ease.scene.commands.DestroyNode
import dev.chieppa.ease.scene.commands.SpawnNode
import dev.chieppa.ease.scene.document.Instance
import dev.chieppa.ease.scene.document.SceneDocument
import dev.chieppa.ease.scene.files.SceneFolder
import dev.chieppa.ease.scene.node.LocalId
import dev.chieppa.ease.scene.node.NodeKey
import dev.chieppa.ease.scene.resolve.SceneProblem
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.io.TempDir

/** Scenes keep going through missing and broken files, and their changes reach snapshots. */
class SceneRecoveryTest {
  @TempDir lateinit var folder: Path

  private val caveGuid = Guid(0, 1)
  private val crateGuid = Guid(0, 2)
  private val cave by lazy { folder.resolve("cave.scene") }
  private val crate by lazy { folder.resolve("crate.prefab") }
  private val crateDocument = document(node(10, "Box"), node(11, "Lid", parent = 10))

  private fun key(vararg ids: Long) =
    NodeKey.of(ids.dropLast(1).map(::LocalId), LocalId(ids.last()))

  private fun save(path: Path, document: SceneDocument, guid: Guid): List<String> {
    val save = SceneFolder.write(path, document)
    MetaFiles.write(MetaFiles.metaOf(path), AssetMeta(guid = guid))
    return save.written
  }

  private fun saveCave() =
    save(
      cave,
      document(node(1, "Hall", x = 1f), node(4, "Crate", instance = Instance(crateGuid))),
      caveGuid,
    )

  private fun ScenePeer.touch(path: Path) = watcher.publish(path, FileChangeKind.MODIFIED)

  /** The node file in [folder]'s `nodes` whose name starts with [prefix]. */
  private fun nodeFile(folder: Path, prefix: String): Path =
    Files.list(folder.resolve("nodes")).use { list ->
      list.filter { it.fileName.toString().startsWith(prefix) }.findFirst().get()
    }

  /** Breaks the YAML of the node file in [folder] whose name starts with [prefix]. */
  private fun breakFile(folder: Path, prefix: String) {
    Files.writeString(nodeFile(folder, prefix), "name: [unclosed\n")
  }

  private fun SpawnNode.key(): NodeKey = snapshot.components.filterIsInstance<NodeKey>().single()

  @Test
  fun `a prefab missing when the scene is placed is picked up once it appears`() {
    saveCave()
    val host = ScenePeer(folder)
    host.scenes.place(caveGuid, tick = 0)
    assertTrue(key(4, 10).text !in host.keys())

    save(crate, crateDocument, crateGuid).forEach { host.touch(crate.resolve(it)) }
    host.touch(MetaFiles.metaOf(crate))
    host.beforeTick(1)

    assertTrue(key(4, 10).text in host.keys())
    assertTrue(key(4, 11).text in host.keys())
  }

  @Test
  fun `a prefab and a scene that fail their first load are picked up once fixed`() {
    save(crate, crateDocument, crateGuid)
    saveCave()
    breakFile(crate, "lid-")
    breakFile(cave, "hall-")
    val host = ScenePeer(folder)
    host.scenes.place(caveGuid, tick = 0)
    assertEquals(emptyList(), host.keys())

    save(crate, crateDocument, crateGuid).forEach { host.touch(crate.resolve(it)) }
    saveCave().forEach { host.touch(cave.resolve(it)) }
    host.beforeTick(1)

    assertTrue(key(1).text in host.keys())
    assertTrue(key(4, 11).text in host.keys())
  }

  @Test
  fun `a component that stops loading keeps its last good value on the live node`() {
    save(crate, crateDocument, crateGuid)
    saveCave()
    val host = ScenePeer(folder)
    host.scenes.place(caveGuid, tick = 0)
    val hall = nodeFile(cave, "hall-")
    Files.writeString(hall, Files.readString(hall).replace("x: 1.0", "x: \"far\""))
    host.touch(hall)
    host.beforeTick(1)

    assertEquals(1f, host.x(key(1)))
    val placed = host.scenes.scenes.single()
    val problems = placed.problems
    assertTrue(problems.any { it is SceneProblem.BadComponent && it.component == Scenes.TRANSFORM })
  }

  @Test
  fun `removed nodes go children first, after their parents were spawned first`() {
    save(crate, crateDocument, crateGuid)
    save(
      cave,
      document(node(9, "Shelf"), node(2, "Jar", parent = 9), node(1, "Lid", parent = 2)),
      caveGuid,
    )
    val host = ScenePeer(folder)
    host.scenes.place(caveGuid, tick = 0)
    val log = host.history.log
    val spawned = log.all.map { log.commandOf(it) }.filterIsInstance<SpawnNode>()
    assertEquals(listOf(key(9), key(2), key(1)), spawned.map { it.key() })

    SceneFolder.write(cave, document()).deleted.forEach { host.touch(cave.resolve(it)) }
    host.beforeTick(1)

    val later = log.all.drop(spawned.size).map { log.commandOf(it) }
    val removed = later.filterIsInstance<DestroyNode>()
    assertEquals(listOf(key(1), key(2), key(9)), removed.map { it.node })
  }

  @Test
  fun `a reload changes replicated components through Replication write, guard satisfied`() {
    save(crate, crateDocument, crateGuid)
    saveCave()
    val host = ScenePeer(folder)
    host.scenes.place(caveGuid, tick = 0)
    host.world.update(0f)
    host.world.replication.clearDirty()

    val moved = SceneFolder.read(cave).with(node(1, "Hall", x = 5f))
    SceneFolder.write(cave, moved).written.forEach { host.touch(cave.resolve(it)) }
    host.beforeTick(1)
    host.world.update(0f)

    val hall = host.node(key(1))
    assertEquals(5f, host.x(key(1)))
    assertTrue(host.world.replication.isDirty(hall, LocalTransform))
  }
}

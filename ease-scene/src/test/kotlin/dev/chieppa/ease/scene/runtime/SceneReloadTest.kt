package dev.chieppa.ease.scene.runtime

import dev.chieppa.ease.assets.Guid
import dev.chieppa.ease.assets.meta.AssetMeta
import dev.chieppa.ease.assets.meta.MetaFiles
import dev.chieppa.ease.ecs.replication.PeerId
import dev.chieppa.ease.platform.files.FileChangeKind
import dev.chieppa.ease.scene.Scenes
import dev.chieppa.ease.scene.Scenes.document
import dev.chieppa.ease.scene.Scenes.id
import dev.chieppa.ease.scene.Scenes.node
import dev.chieppa.ease.scene.document.Instance
import dev.chieppa.ease.scene.document.OverridePath
import dev.chieppa.ease.scene.document.SceneDocument
import dev.chieppa.ease.scene.files.SceneFolder
import dev.chieppa.ease.scene.node.LocalId
import dev.chieppa.ease.scene.node.NodeKey
import dev.chieppa.ease.scene.node.NodeName
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.io.TempDir

/**
 * A scene changed on disk reaches a running host at the tick boundary as commands, and the same
 * commands bring a client to the same nodes.
 */
class SceneReloadTest {
  @TempDir lateinit var folder: Path

  private val caveGuid = Guid(0, 1)
  private val crateGuid = Guid(0, 2)
  private val cave by lazy { folder.resolve("cave.scene") }
  private val crate by lazy { folder.resolve("crate.prefab") }

  private fun key(vararg ids: Long) =
    NodeKey.of(ids.dropLast(1).map(::LocalId), LocalId(ids.last()))

  private fun save(path: Path, document: SceneDocument, guid: Guid) {
    SceneFolder.write(path, document)
    MetaFiles.write(MetaFiles.metaOf(path), AssetMeta(guid = guid))
  }

  private fun project() {
    save(crate, document(node(10, "Box"), node(11, "Lid", parent = 10)), crateGuid)
    val raised = OverridePath(listOf(id(11)), Scenes.TRANSFORM, "x")
    save(
      cave,
      document(
        node(1, "Hall"),
        node(2, "Lamp", parent = 1, x = 1f),
        node(3, "Rope", parent = 1),
        node(4, "Crate A", parent = 1, instance = Instance(crateGuid)),
        node(
          5,
          "Crate B",
          parent = 1,
          instance = Instance(crateGuid).withOverride(raised, JsonPrimitive(8)),
        ),
      ),
      caveGuid,
    )
  }

  private fun changed(peer: ScenePeer, path: Path) =
    peer.watcher.publish(path, FileChangeKind.MODIFIED)

  @Test
  fun `placing a scene spawns its nodes on the host, and its commands spawn them on a client`() {
    project()
    val host = ScenePeer(folder)
    val client = ScenePeer(folder, PeerId(1))

    host.scenes.place(caveGuid, tick = 0)
    client.receiveFrom(host)

    val expected =
      listOf(key(1), key(2), key(3), key(4), key(4, 10), key(4, 11), key(5), key(5, 10), key(5, 11))
        .map { it.text }
        .sorted()
    assertEquals(expected, host.keys())
    assertEquals(expected, client.keys())
    assertEquals(8f, client.x(key(5, 11)))
    val placed = host.scenes.scenes.single()
    assertEquals(emptyList(), placed.problems)
  }

  @Test
  fun `reloading the scene and its prefab changes both host and client, overrides kept`() {
    project()
    val host = ScenePeer(folder)
    val client = ScenePeer(folder, PeerId(1))
    host.scenes.place(caveGuid, tick = 0)
    client.receiveFrom(host)

    val scene = SceneFolder.read(cave)
    val edited =
      scene
        .with(node(2, "Lamp", parent = 1, x = 6f))
        .without(id(3))
        .with(node(7, "Banner", parent = 1, order = scene.nextOrder(id(1))))
    val written = SceneFolder.write(cave, edited)
    val crateSave =
      SceneFolder.write(crate, document(node(10, "Box"), node(11, "Lid", parent = 10, x = 3f)))
    (written.written + written.deleted).forEach { changed(host, cave.resolve(it)) }
    crateSave.written.forEach { changed(host, crate.resolve(it)) }
    host.beforeTick(1)
    client.receiveFrom(host)

    for (peer in listOf(host, client)) {
      assertEquals(6f, peer.x(key(2)))
      assertTrue(key(3).text !in peer.keys())
      assertEquals("Banner", with(peer.world) { peer.node(key(7))[NodeName].name })
      assertEquals(3f, peer.x(key(4, 11)))
      assertEquals(8f, peer.x(key(5, 11)))
    }
  }

  @Test
  fun `a tick with no file change sends no command`() {
    project()
    val host = ScenePeer(folder)
    host.scenes.place(caveGuid, tick = 0)
    val log = host.history.log
    val logged = log.all.size

    host.beforeTick(1)
    changed(host, cave.resolve("scene.yaml"))
    host.beforeTick(2)

    assertEquals(logged, log.all.size)
  }
}

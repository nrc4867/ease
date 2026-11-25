package dev.chieppa.ease.scene.runtime

import com.github.quillraven.fleks.wildcardSnapshotOf
import dev.chieppa.ease.assets.Guid
import dev.chieppa.ease.assets.meta.AssetMeta
import dev.chieppa.ease.assets.meta.MetaFiles
import dev.chieppa.ease.commands.CommandOrigin
import dev.chieppa.ease.ecs.LocalTransform
import dev.chieppa.ease.ecs.replication.Authority
import dev.chieppa.ease.ecs.replication.PeerId
import dev.chieppa.ease.platform.files.FileChangeKind
import dev.chieppa.ease.scene.Beacon
import dev.chieppa.ease.scene.Panel
import dev.chieppa.ease.scene.Scenes
import dev.chieppa.ease.scene.Scenes.document
import dev.chieppa.ease.scene.Scenes.fields
import dev.chieppa.ease.scene.Scenes.id
import dev.chieppa.ease.scene.Scenes.node
import dev.chieppa.ease.scene.commands.SpawnNode
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.io.TempDir

/** A reload writes only the fields whose file value changed since the version last applied. */
class SceneFieldReloadTest {
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

  private fun cave(vararg nodes: dev.chieppa.ease.scene.document.SceneNode) =
    document(node(1, "Hall"), *nodes)

  private fun placed(scene: SceneDocument): ScenePeer {
    save(crate, document(node(10, "Box", x = 2f)), crateGuid)
    save(cave, scene, caveGuid)
    return ScenePeer(folder).also { it.scenes.place(caveGuid, tick = 0) }
  }

  private fun ScenePeer.reload(path: Path, edited: SceneDocument, tick: Long) {
    val saved = SceneFolder.write(path, edited)
    (saved.written + saved.deleted).forEach {
      watcher.publish(path.resolve(it), FileChangeKind.MODIFIED)
    }
    beforeTick(tick)
  }

  private fun ScenePeer.move(key: NodeKey, x: Float? = null, y: Float? = null) {
    with(world) {
      val transform = node(key)[LocalTransform]
      x?.let { transform.x = it }
      y?.let { transform.y = it }
    }
  }

  private fun ScenePeer.setRight(key: NodeKey, value: Float) {
    with(world) { node(key)[Panel].margin.right = value }
  }

  private fun ScenePeer.y(key: NodeKey): Float = with(world) { node(key)[LocalTransform].y }

  private fun edit(scene: SceneDocument, n: Long, field: String, value: Float) =
    scene.with(scene.nodes.getValue(id(n)).withField(Scenes.TRANSFORM, field, JsonPrimitive(value)))

  @Test
  fun `editing one field of one node writes only that field and keeps live state`() {
    val scene = cave(node(2, "A", parent = 1, x = 1f), node(3, "B", parent = 1, x = 5f))
    val host = placed(scene)
    host.move(key(2), y = 9f)
    host.move(key(3), x = 77f, y = 4f)

    host.reload(cave, edit(scene, 2, "x", 6f), 1)

    assertEquals(6f, host.x(key(2)))
    assertEquals(9f, host.y(key(2)))
    assertEquals(77f, host.x(key(3)))
    assertEquals(4f, host.y(key(3)))
  }

  @Test
  fun `a node whose file did not change keeps its drifted live value`() {
    val scene = cave(node(2, "A", parent = 1, x = 1f))
    val host = placed(scene)
    host.move(key(2), x = 50f)

    host.reload(cave, scene.with(node(3, "C", parent = 1, x = 3f)), 1)

    assertEquals(50f, host.x(key(2)))
    assertEquals(3f, host.x(key(3)))
  }

  @Test
  fun `removing a node destroys it and leaves the others`() {
    val scene = cave(node(2, "A", parent = 1, x = 1f), node(3, "B", parent = 1, x = 5f))
    val host = placed(scene)
    host.move(key(3), x = 8f)

    host.reload(cave, scene.without(id(2)), 1)

    assertEquals(listOf(key(1).text, key(3).text).sorted(), host.keys())
    assertEquals(8f, host.x(key(3)))
  }

  @Test
  fun `a component added in a file is set and one removed is removed`() {
    val scene = cave(node(2, "A", parent = 1, x = 1f))
    val host = placed(scene)
    val beacon = fields("strength" to 3)
    val added = scene.nodes.getValue(id(2))

    host.reload(
      cave,
      scene.with(added.copy(components = added.components + (Scenes.BEACON to beacon))),
      1,
    )
    assertEquals(3f, with(host.world) { host.node(key(2))[Beacon].strength })

    host.reload(cave, scene, 2)
    with(host.world) { assertNull(host.node(key(2)).getOrNull(Beacon)) }
  }

  @Test
  fun `a file that failed to decode and is fixed applies the fixed value`() {
    val scene = cave(node(2, "A", parent = 1, x = 1f))
    val host = placed(scene)
    host.move(key(2), y = 9f)
    val bad = scene.nodes.getValue(id(2)).withField(Scenes.TRANSFORM, "x", JsonPrimitive("far"))

    host.reload(cave, scene.with(bad), 1)
    assertEquals(1f, host.x(key(2)))
    host.reload(cave, edit(scene, 2, "x", 4f), 2)

    assertEquals(4f, host.x(key(2)))
    assertEquals(9f, host.y(key(2)))
  }

  @Test
  fun `a component that failed to decode and is then deleted from the file is removed`() {
    val node = node(2, "A", parent = 1, x = 1f)
    val withBeacon =
      node.copy(components = node.components + (Scenes.BEACON to fields("strength" to 3)))
    val scene = cave(withBeacon)
    val host = placed(scene)
    val bad = withBeacon.withField(Scenes.BEACON, "strength", JsonPrimitive("far"))

    host.reload(cave, scene.with(bad), 1)
    assertEquals(3f, with(host.world) { host.node(key(2))[Beacon].strength })
    host.reload(cave, scene.with(node), 2)

    with(host.world) { assertNull(host.node(key(2)).getOrNull(Beacon)) }
  }

  @Test
  fun `a change the world rejected is tried again on the next reload`() {
    val scene = cave(node(2, "A", parent = 1, x = 1f), node(3, "B", parent = 1, x = 5f))
    val host = placed(scene)
    val foreign = PeerId(7)
    with(host.world) { host.node(key(2))[Authority].holder = foreign }

    host.reload(cave, edit(scene, 2, "x", 6f), 1)
    assertEquals(1f, host.x(key(2)))
    with(host.world) { host.node(key(2))[Authority].holder = PeerId.HOST }
    host.reload(cave, edit(edit(scene, 2, "x", 6f), 3, "x", 7f), 2)

    assertEquals(6f, host.x(key(2)))
    assertEquals(7f, host.x(key(3)))
  }

  @Test
  fun `a live node the last version lacked loses scene components its file lacks`() {
    val scene = cave(node(2, "A", parent = 1, x = 1f))
    val host = placed(scene)
    val extra = key(9)
    host.history.execute(
      SpawnNode(
        wildcardSnapshotOf(
          listOf(
            extra,
            NodeName("Extra"),
            Beacon(strength = 4f),
            LocalTransform().apply { x = 3f },
          ),
          emptyList(),
        )
      ),
      0,
      CommandOrigin.GAME,
    )
    host.reload(cave, scene.with(node(9, "Extra", parent = 1, x = 8f)), 1)

    assertEquals(8f, host.x(extra))
    with(host.world) { assertNull(host.node(extra).getOrNull(Beacon)) }
  }

  @Test
  fun `a prefab edit reaches instances while their overrides win and other fields stay`() {
    val raised = OverridePath(listOf(id(10)), Scenes.TRANSFORM, "x")
    val scene =
      cave(
        node(4, "A", parent = 1, instance = Instance(crateGuid)),
        node(
          5,
          "B",
          parent = 1,
          instance = Instance(crateGuid).withOverride(raised, JsonPrimitive(8)),
        ),
      )
    val host = placed(scene)
    host.move(key(4, 10), y = 3f)

    host.reload(crate, edit(document(node(10, "Box", x = 2f)), 10, "x", 7f), 1)

    assertEquals(7f, host.x(key(4, 10)))
    assertEquals(3f, host.y(key(4, 10)))
    assertEquals(8f, host.x(key(5, 10)))
    assertNotNull(host.scenes.placementOf(key(5, 10)))
  }

  @Test
  fun `a nested field changes alone, its siblings keeping their live values`() {
    val node = node(2, "A", parent = 1)
    val margin = JsonObject(mapOf("left" to JsonPrimitive(1f), "right" to JsonPrimitive(2f)))
    val panel =
      node.copy(
        components = node.components + (Scenes.PANEL to JsonObject(mapOf("margin" to margin)))
      )
    val scene = cave(panel)
    val host = placed(scene)
    host.setRight(key(2), 9f)

    val moved = JsonObject(mapOf("left" to JsonPrimitive(4f), "right" to JsonPrimitive(2f)))
    host.reload(
      cave,
      scene.with(
        panel.copy(
          components = panel.components + (Scenes.PANEL to JsonObject(mapOf("margin" to moved)))
        )
      ),
      1,
    )

    with(host.world) {
      val live = host.node(key(2))[Panel].margin
      assertEquals(4f, live.left)
      assertEquals(9f, live.right)
    }
  }
}

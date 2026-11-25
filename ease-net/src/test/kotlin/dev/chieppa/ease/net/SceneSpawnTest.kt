package dev.chieppa.ease.net

import com.github.quillraven.fleks.Entity
import dev.chieppa.ease.assets.Guid
import dev.chieppa.ease.ecs.replication.Authority
import dev.chieppa.ease.ecs.replication.PeerId
import dev.chieppa.ease.ecs.replication.networkIdOf
import dev.chieppa.ease.ecs.replication.replication
import dev.chieppa.ease.net.session.CloseReason
import dev.chieppa.ease.net.view.ViewRefusal
import dev.chieppa.ease.scene.document.Instance
import dev.chieppa.ease.scene.document.OverridePath
import dev.chieppa.ease.scene.document.SceneDocument
import dev.chieppa.ease.scene.document.SceneNode
import dev.chieppa.ease.scene.files.SceneFolder
import dev.chieppa.ease.scene.node.LocalId
import dev.chieppa.ease.scene.node.NodeKey
import dev.chieppa.ease.scene.node.NodeName
import dev.chieppa.ease.scene.node.OrderKey
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.io.TempDir

/**
 * A client's scene nodes come from the host's spawns, which name the placed asset and each node's
 * key: the client builds them from its own files, overrides applied, under the host's network ids
 * and held by the host. Reloads reach it as snapshots, spawns and despawns.
 */
class SceneSpawnTest {
  @TempDir lateinit var folder: Path

  private val caveGuid = Guid(0, 1)
  private val crateGuid = Guid(0, 2)
  private val cave by lazy { folder.resolve("cave.scene") }
  private val crate by lazy { folder.resolve("crate.prefab") }

  private fun key(vararg ids: Long) =
    NodeKey.of(ids.dropLast(1).map(::LocalId), LocalId(ids.last()))

  private fun node(
    id: Long,
    name: String,
    parent: Long? = null,
    x: Float = 0f,
    instance: Instance? = null,
  ): SceneNode {
    val transform = JsonObject(mapOf("x" to JsonPrimitive(x)))
    val parentId = parent?.let(::LocalId)
    return SceneNode(
      LocalId(id),
      name,
      parentId,
      OrderKey.FIRST,
      mapOf(TRANSFORM to transform),
      instance,
    )
  }

  private fun document(vararg nodes: SceneNode) = SceneDocument(nodes = nodes.toList())

  private fun project() {
    ScenePair.save(crate, document(node(10, "Box"), node(11, "Lid", parent = 10)), crateGuid)
    val raised = OverridePath(listOf(LocalId(11)), TRANSFORM, "x")
    ScenePair.save(
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

  @Test
  fun `a placed scene's nodes reach the client built from its own files`() {
    project()
    val pair = ScenePair(folder)
    pair.scenes.place(caveGuid, tick = 0)
    pair.step()

    assertEquals(pair.host.keys(), pair.client.keys())
    assertEquals(9, pair.client.keys().size)
    for (text in pair.host.keys()) {
      val key = NodeKey.parse(text)
      val replica = pair.client.node(key)
      assertEquals(
        pair.host.world.networkIdOf(pair.host.node(key)),
        pair.client.world.networkIdOf(replica),
      )
      assertEquals(PeerId.HOST, with(pair.client.world) { replica[Authority].holder })
      val replication = pair.client.world.replication
      assertFalse(replication.hasAuthority(replica))
    }
    assertEquals(8f, pair.client.x(key(5, 11)))
    assertEquals("Lid", with(pair.client.world) { pair.client.node(key(4, 11))[NodeName].name })
  }

  @Test
  fun `reloads reach the client as snapshots, spawns and despawns`() {
    project()
    val pair = ScenePair(folder)
    pair.scenes.place(caveGuid, tick = 0)
    pair.step()

    val scene = SceneFolder.read(cave)
    val banner = node(7, "Banner", parent = 1).copyWithOrder(scene.nextOrder(LocalId(1)))
    pair.edit(
      cave,
      scene.with(node(2, "Lamp", parent = 1, x = 6f)).without(LocalId(3)).with(banner),
    )
    pair.edit(crate, document(node(10, "Box"), node(11, "Lid", parent = 10, x = 3f)))
    repeat(SETTLE) { pair.step() }

    assertEquals(pair.host.keys(), pair.client.keys())
    assertEquals(6f, pair.client.x(key(2)))
    assertEquals(Entity.NONE, pair.client.node(key(3)))
    assertEquals("Banner", with(pair.client.world) { pair.client.node(key(7))[NodeName].name })
    assertEquals(3f, pair.client.x(key(4, 11)))
    assertEquals(8f, pair.client.x(key(5, 11)))
    assertTrue(pair.clientSession.isOpen)
  }

  @Test
  fun `a client whose files lack the placed scene closes with a content mismatch`(
    @TempDir elsewhere: Path
  ) {
    project()
    val pair = ScenePair(folder, clientRoot = elsewhere)
    pair.scenes.place(caveGuid, tick = 0)
    pair.step()

    assertFalse(pair.clientSession.isOpen)
    assertEquals(CloseReason.CONTENT_MISMATCH, pair.clientSession.closeReason)
    assertEquals(ViewRefusal.UNKNOWN_ASSET, pair.clientSession.viewRefusal)
    assertNotEquals(0, pair.host.keys().size)
    assertEquals(emptyList(), pair.client.keys())
  }

  private fun SceneNode.copyWithOrder(order: OrderKey) =
    SceneNode(id, name, parent, order, components, instance)

  private companion object {
    const val TRANSFORM = "ease.LocalTransform"
    const val SETTLE = 3
  }
}

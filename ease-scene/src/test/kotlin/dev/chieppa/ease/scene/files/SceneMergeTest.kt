package dev.chieppa.ease.scene.files

import dev.chieppa.ease.scene.Scenes
import dev.chieppa.ease.scene.Scenes.document
import dev.chieppa.ease.scene.Scenes.id
import dev.chieppa.ease.scene.Scenes.node
import dev.chieppa.ease.scene.document.SceneDocument
import dev.chieppa.ease.scene.document.SceneNode
import dev.chieppa.ease.scene.node.LocalId
import dev.chieppa.ease.scene.node.LocalIdSource
import dev.chieppa.ease.scene.node.OrderKey
import java.nio.file.Path
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.io.TempDir

/**
 * Two branches change one scene in plain git, with no merge driver: edits, additions and reorders
 * of different nodes merge without a conflict, and the merged folder reads as both.
 */
class SceneMergeTest {
  @TempDir lateinit var folder: Path

  private val repo by lazy { GitRepo.init(folder) }
  private val scene by lazy { folder.resolve("cave.scene") }

  /** A hall with four children in order: Lamp, Door, Torch, Chest. */
  private val base: SceneDocument by lazy {
    var order = OrderKey.FIRST
    val children =
      listOf(2L to "Lamp", 3L to "Door", 4L to "Torch", 5L to "Chest").map { (id, name) ->
        node(id, name, parent = 1, order = order).also { order = OrderKey.after(order) }
      }
    document(node(1, "Hall"), *children.toTypedArray())
  }

  private fun SceneDocument.edit(id: Long, change: (SceneNode) -> SceneNode) =
    with(change(checkNotNull(this[id(id)])))

  private fun SceneDocument.moveX(id: Long, x: Int) =
    edit(id) {
      it.withField(Scenes.TRANSFORM, "x", JsonPrimitive(x))
    }

  /** Moves node [id] to just before the first child of the hall. */
  private fun SceneDocument.toFront(id: Long) =
    edit(id) {
      it.copy(order = OrderKey.between(null, children(id(1)).first().order))
    }

  /** Adds a child of the hall with a new random id after its last child. */
  private fun SceneDocument.addChild(name: String, ids: LocalIdSource) =
    with(SceneNode(ids.next(), name, id(1), nextOrder(id(1))))

  private fun names(document: SceneDocument) = document.children(id(1)).map { it.name }

  private fun branch(name: String, change: (SceneDocument) -> SceneDocument) {
    repo.git("checkout", "--quiet", "-b", name, "main")
    SceneFolder.write(scene, change(SceneFolder.read(scene)))
    repo.commitAll(name)
  }

  @Test
  fun `edits, additions and reorders of different nodes merge with no conflict`() {
    SceneFolder.write(scene, base)
    repo.commitAll("base")
    branch("left") { it.moveX(2, 7).toFront(4).addChild("Banner", ids(1)) }
    branch("right") { it.moveX(3, 9).toFront(5).addChild("Rug", ids(2)) }

    val merge = repo.run("merge", "--no-edit", "left")

    assertEquals(0, merge.exitCode, merge.output)
    assertEquals("", repo.git("diff", "--name-only", "--diff-filter=U"))
    val merged = SceneFolder.read(scene)
    assertEquals(JsonPrimitive(7), fieldX(merged, 2))
    assertEquals(JsonPrimitive(9), fieldX(merged, 3))
    assertEquals(
      listOf("Torch", "Chest", "Lamp", "Door"),
      names(merged).filter { it in BASE_NAMES },
    )
    assertEquals(setOf("Banner", "Rug"), names(merged).filter { it !in BASE_NAMES }.toSet())
  }

  @Test
  fun `two edits of the same node conflict in that node's file only`() {
    SceneFolder.write(scene, base)
    repo.commitAll("base")
    branch("left") { it.moveX(2, 7).moveX(3, 1) }
    branch("right") { it.moveX(2, 8).moveX(4, 1) }

    val merge = repo.run("merge", "--no-edit", "left")

    assertNotEquals(0, merge.exitCode)
    assertEquals(
      "cave.scene/nodes/0000000000000002.yaml",
      repo.git("diff", "--name-only", "--diff-filter=U").trim(),
    )
  }

  private fun fieldX(document: SceneDocument, id: Long) =
    document[id(id)]?.components?.get(Scenes.TRANSFORM)?.get("x")

  /** Random ids from a seeded source, as two people adding nodes on two machines get. */
  private fun ids(seed: Int): LocalIdSource {
    val random = Random(seed)
    return LocalIdSource { LocalId(random.nextLong()) }
  }

  private companion object {
    val BASE_NAMES = setOf("Lamp", "Door", "Torch", "Chest")
  }
}

package dev.chieppa.ease.scene.files

import dev.chieppa.ease.assets.Guid
import dev.chieppa.ease.scene.Scenes
import dev.chieppa.ease.scene.Scenes.document
import dev.chieppa.ease.scene.Scenes.id
import dev.chieppa.ease.scene.Scenes.node
import dev.chieppa.ease.scene.document.Instance
import dev.chieppa.ease.scene.document.OverridePath
import dev.chieppa.ease.scene.node.OrderKey
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.io.TempDir

class SceneFilesTest {
  @TempDir lateinit var folder: Path

  private val scene by lazy { folder.resolve("cave.scene") }

  private val cave =
    document(
      node(1, "Hall"),
      node(2, "Lamp", parent = 1, x = 2.5f),
      node(3, "Door", parent = 1, order = OrderKey.after(OrderKey.FIRST)),
    )

  @Test
  fun `a node file holds its name, parent, order and components, keys sorted`() {
    val files = SceneFiles.encode(cave)

    val expected =
      listOf(
          "components:",
          "  ease.LocalTransform:",
          "    x: 2.5",
          "formatVersion: 1",
          "name: Lamp",
          "order: \"n\"",
          "parent: \"0000000000000001\"",
        )
        .joinToString("\n", postfix = "\n")
    assertEquals(expected, files["nodes/0000000000000002.yaml"])
    assertEquals("formatVersion: 1\nsettings: {}\n", files["scene.yaml"])
    assertEquals(4, files.size)
  }

  @Test
  fun `a saved scene reads back equal`() {
    SceneFolder.write(scene, cave)

    assertEquals(cave, SceneFolder.read(scene))
  }

  @Test
  fun `saving an unchanged scene, or one read back, writes nothing`() {
    SceneFolder.write(scene, cave)

    assertTrue(SceneFolder.write(scene, cave).unchanged)
    assertTrue(SceneFolder.write(scene, SceneFolder.read(scene)).unchanged)
  }

  @Test
  fun `an edit rewrites only the changed node's file`() {
    SceneFolder.write(scene, cave)
    val moved =
      cave.with(checkNotNull(cave[id(3)]).withField(Scenes.TRANSFORM, "x", JsonPrimitive(4)))

    val save = SceneFolder.write(scene, moved)

    assertEquals(listOf("nodes/0000000000000003.yaml"), save.written)
    assertEquals(emptyList(), save.deleted)
  }

  @Test
  fun `removing a node deletes its file and nothing else`() {
    SceneFolder.write(scene, cave)

    val save = SceneFolder.write(scene, cave.without(id(2)))

    assertEquals(listOf("nodes/0000000000000002.yaml"), save.deleted)
    assertEquals(emptyList(), save.written)
    assertFalse(Files.exists(scene.resolve("nodes/0000000000000002.yaml")))
  }

  @Test
  fun `an instance's overrides are one line each, sorted by path`() {
    val prefab = Guid(0, 9)
    val instance =
      Instance(prefab)
        .withOverride(OverridePath(listOf(id(5)), Scenes.TRANSFORM, "y"), JsonPrimitive(1))
        .withOverride(OverridePath(listOf(id(4)), Scenes.TRANSFORM, "x"), JsonPrimitive(2))
    val placed = document(node(7, "Crate", instance = instance))

    val text = SceneFiles.encode(placed).getValue("nodes/0000000000000007.yaml")

    val fromOverrides = text.lines().dropWhile { it != "  overrides:" }
    val overrides = fromOverrides.drop(1).takeWhile { it.startsWith("    ") }
    assertEquals(
      listOf(
        "    \"0000000000000004/ease.LocalTransform/x\": 2",
        "    \"0000000000000005/ease.LocalTransform/y\": 1",
      ),
      overrides,
    )
    assertEquals(placed, SceneFiles.decode(SceneFiles.encode(placed)))
  }

  @Test
  fun `a file from a newer format, or without one, is refused naming the file`() {
    val files = SceneFiles.encode(cave).toMutableMap()
    val lamp = "nodes/0000000000000002.yaml"
    files[lamp] = files.getValue(lamp).replace("formatVersion: 1", "formatVersion: 2")

    val newer = assertFailsWith<SceneFileException> { SceneFiles.decode(files) }
    assertEquals(lamp, newer.file)

    files[lamp] = files.getValue(lamp).replace("formatVersion: 2\n", "")
    assertEquals(lamp, assertFailsWith<SceneFileException> { SceneFiles.decode(files) }.file)
  }

  @Test
  fun `a node file not named by a local id is refused`() {
    val files = SceneFiles.encode(cave).toMutableMap()
    files["nodes/lamp.yaml"] = files.getValue("nodes/0000000000000002.yaml")

    assertEquals(
      "nodes/lamp.yaml",
      assertFailsWith<SceneFileException> { SceneFiles.decode(files) }.file,
    )
  }
}

package dev.chieppa.ease.project.check

import dev.chieppa.ease.project.TestGit
import dev.chieppa.ease.project.TestProject
import dev.chieppa.ease.project.TestProject.Companion.guid
import dev.chieppa.ease.project.TestProject.Companion.node
import dev.chieppa.ease.scene.node.OrderKey
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.jupiter.api.io.TempDir

class SceneRulesFilesTest {
  @TempDir lateinit var folder: Path

  private val project by lazy { TestProject(folder) }

  private fun report() = ProjectChecker(folder, TestGit.isolated(folder)).check()

  private fun nodeFile(prefix: String): Path =
    Files.list(folder.resolve("content/main.scene/nodes")).use { list ->
      list.toList().first { it.fileName.toString().startsWith(prefix) }
    }

  @Test
  fun `a node file with an invalid value is unreadable, not a repeated node id`() {
    project.scene("main.scene", guid(2), listOf(node(1, "Hall")))
    val file = nodeFile("hall-")
    val text = Files.readString(file)
    Files.writeString(file, text.replace(Regex("(?m)^order: .*$"), "order: \"b as c\""))

    val finding = report().findings.single()

    assertEquals(FindingKind.UNREADABLE_SCENE_FILE, finding.kind)
  }

  @Test
  fun `a node whose parent file holds conflict markers is not missing its parent`() {
    val later = OrderKey.after(OrderKey.FIRST)
    project.scene(
      "main.scene",
      guid(2),
      listOf(node(1, "Hall"), node(2, "Lamp", parent = 1, order = later)),
    )
    val hall = nodeFile("hall-")
    val text = Files.readString(hall)
    val theirs = text.replace("Hall", "Great")
    Files.writeString(hall, "<<<<<<< ours\n$text=======\n$theirs>>>>>>> theirs\n")

    val report = report()

    assertEquals(listOf(FindingKind.CONFLICT_MARKERS), report.findings.map { it.kind })
  }
}

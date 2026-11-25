package dev.chieppa.ease.project.check

import dev.chieppa.ease.project.TestGit
import dev.chieppa.ease.project.TestProject
import dev.chieppa.ease.project.TestProject.Companion.guid
import dev.chieppa.ease.project.TestProject.Companion.node
import dev.chieppa.ease.project.fix.ProjectFixer
import dev.chieppa.ease.scene.node.LocalId
import dev.chieppa.ease.scene.node.OrderKey
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.io.TempDir

class ProjectFixerTest {
  @TempDir lateinit var folder: Path

  private val project by lazy { TestProject(folder) }
  private val git by lazy { TestGit.isolated(folder) }

  private fun check() = ProjectChecker(folder, git).check()

  @Test
  fun `equal order keys are respaced and the siblings keep their order`() {
    val key = OrderKey.FIRST
    project.scene(
      "main.scene",
      guid(2),
      listOf(
        node(1, "Hall"),
        node(2, "A", parent = 1, order = key),
        node(3, "B", parent = 1, order = key),
      ),
    )

    val report = ProjectFixer(folder, git).fix()

    assertEquals(1, report.changes.size)
    assertEquals(emptyList(), check().findings)
    assertEquals(
      listOf("A", "B"),
      project.readScene("main.scene").children(LocalId(1)).map { it.name },
    )
  }

  @Test
  fun `a sidecar conflicted between two GUIDs keeps ours and points the references at it`() {
    project.asset("shared.txt", "s", guid(1))
    Files.writeString(
      folder.resolve("content/shared.txt.meta"),
      "<<<<<<< ours\nformatVersion: 1\nguid: ${guid(1)}\n=======\n" +
        "formatVersion: 1\nguid: ${guid(5)}\n>>>>>>> theirs\n",
    )
    project.scene(
      "main.scene",
      guid(2),
      listOf(
        node(1, "A", asset = guid(1)),
        node(2, "B", asset = guid(5), order = OrderKey.after(OrderKey.FIRST)),
      ),
    )

    val report = ProjectFixer(folder, git).fix()

    assertTrue(report.changes.any { it.contains("kept GUID ${guid(1)}") }, "${report.changes}")
    assertEquals(emptyList(), check().findings)
    val nodes = project.readScene("main.scene").nodes.values
    val slots = nodes.map { it.components["test.Slot"].toString() }
    assertTrue(slots.all { it.contains(guid(1).toString()) }, "$slots")
  }

  @Test
  fun `an asset deleted in history is restored for the references that remain`() {
    git.require("init", "--quiet")
    project.asset("rock.txt", "rock", guid(1))
    TestGit.commitAll(git, "add rock")
    Files.delete(folder.resolve("content/rock.txt"))
    Files.delete(folder.resolve("content/rock.txt.meta"))
    project.scene("main.scene", guid(2), listOf(node(1, "Rock", asset = guid(1))))
    TestGit.commitAll(git, "delete rock, place it")

    val before = check().errors.single()
    val report = ProjectFixer(folder, git).fix()

    assertEquals(FindingKind.DANGLING_REFERENCE, before.kind)
    assertTrue(before.message.contains("content/rock.txt.meta was deleted in"), before.message)
    assertTrue(report.changes.single().startsWith("restored"), "${report.changes}")
    assertEquals(emptyList(), check().findings)
  }
}

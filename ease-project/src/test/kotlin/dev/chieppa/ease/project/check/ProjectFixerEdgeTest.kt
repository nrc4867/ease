package dev.chieppa.ease.project.check

import dev.chieppa.ease.project.TestGit
import dev.chieppa.ease.project.TestProject
import dev.chieppa.ease.project.TestProject.Companion.guid
import dev.chieppa.ease.project.TestProject.Companion.node
import dev.chieppa.ease.project.fix.ProjectFixer
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.io.TempDir

class ProjectFixerEdgeTest {
  @TempDir lateinit var folder: Path

  private val project by lazy { TestProject(folder) }
  private val git by lazy { TestGit.isolated(folder) }

  private fun check() = ProjectChecker(folder, git).check()

  @Test
  fun `a sidecar whose sides differ in more than the GUID is left for a person`() {
    project.asset("shared.txt", "s", guid(1))
    val conflicted =
      "<<<<<<< ours\nformatVersion: 1\nguid: ${guid(1)}\n=======\n" +
        "formatVersion: 1\nguid: ${guid(5)}\nimporter:\n  quality: 3\n>>>>>>> theirs\n"
    Files.writeString(folder.resolve("content/shared.txt.meta"), conflicted)

    val report = ProjectFixer(folder, git).fix()

    assertEquals(emptyList(), report.changes)
    assertEquals(conflicted, Files.readString(folder.resolve("content/shared.txt.meta")))
  }

  @Test
  fun `a deleted asset that was imported again points its references at the new GUID`() {
    git.require("init", "--quiet")
    project.asset("rock.txt", "old rock", guid(1))
    TestGit.commitAll(git, "add rock")
    Files.delete(folder.resolve("content/rock.txt"))
    Files.delete(folder.resolve("content/rock.txt.meta"))
    project.scene("main.scene", guid(2), listOf(node(1, "Rock", asset = guid(1))))
    TestGit.commitAll(git, "delete rock, place it")
    project.asset("rock.txt", "new rock", guid(7))
    TestGit.commitAll(git, "import rock again")

    val report = ProjectFixer(folder, git).fix()

    assertTrue(report.changes.single().contains("pointed references"), "${report.changes}")
    assertEquals("new rock", Files.readString(folder.resolve("content/rock.txt")))
    assertEquals(emptyList(), check().findings)
    val node = project.readScene("main.scene").nodes.values.single()
    val slot = node.components["test.Slot"]
    assertTrue(slot.toString().contains(guid(7).toString()), "$slot")
  }

  @Test
  fun `a deleted asset with spaces and non-ASCII letters in its name is restored`() {
    git.require("init", "--quiet")
    project.asset("café rock.txt", "rock", guid(1))
    TestGit.commitAll(git, "add rock")
    Files.delete(folder.resolve("content/café rock.txt"))
    Files.delete(folder.resolve("content/café rock.txt.meta"))
    project.scene("main.scene", guid(2), listOf(node(1, "Rock", asset = guid(1))))
    TestGit.commitAll(git, "delete rock, place it")

    val report = ProjectFixer(folder, git).fix()

    assertTrue(report.changes.single().startsWith("restored"), "${report.changes}")
    assertEquals("rock", Files.readString(folder.resolve("content/café rock.txt")))
    assertEquals(emptyList(), check().findings)
  }
}

package dev.chieppa.ease.project.check

import dev.chieppa.ease.project.TestGit
import dev.chieppa.ease.project.TestProject
import dev.chieppa.ease.project.TestProject.Companion.guid
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.io.TempDir

class GitHistoryTest {
  @TempDir lateinit var folder: Path

  private val git by lazy { TestGit.isolated(folder, oneInstant = true) }
  private val project by lazy { TestProject(folder) }

  @Test
  fun `a deletion is found before the addition it undoes when every commit has one date`() {
    git.require("init", "--quiet", "-b", "main")
    project.asset("rock.txt", "rock", guid(1))
    TestGit.commitAll(git, "add rock")
    git.require("checkout", "--quiet", "-b", "left")
    Files.delete(folder.resolve("content/rock.txt"))
    Files.delete(folder.resolve("content/rock.txt.meta"))
    TestGit.commitAll(git, "delete rock")
    git.require("checkout", "--quiet", "-b", "right", "main")
    project.asset("other.txt", "other", guid(2))
    TestGit.commitAll(git, "add other")

    val sighting = assertNotNull(GitHistory(git).sighting(guid(1)))

    assertTrue(sighting.deleted)
    assertEquals("content/rock.txt.meta", sighting.meta)
    assertEquals(git.require("rev-parse", "left").trim(), sighting.commit)
  }

  @Test
  fun `a path with spaces and non-ASCII letters is read whole`() {
    git.require("init", "--quiet", "-b", "main")
    project.asset("café au lait.txt", "rock", guid(1))
    TestGit.commitAll(git, "add")
    Files.delete(folder.resolve("content/café au lait.txt.meta"))
    Files.delete(folder.resolve("content/café au lait.txt"))
    TestGit.commitAll(git, "delete")

    val sighting = assertNotNull(GitHistory(git).sighting(guid(1)))

    assertEquals("content/café au lait.txt.meta", sighting.meta)
    assertTrue(sighting.deleted)
  }
}

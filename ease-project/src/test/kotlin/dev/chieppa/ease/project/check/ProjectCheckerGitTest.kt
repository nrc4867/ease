package dev.chieppa.ease.project.check

import dev.chieppa.ease.project.TestGit
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.jupiter.api.io.TempDir

class ProjectCheckerGitTest {
  @TempDir lateinit var folder: Path

  private val git by lazy { TestGit.isolated(folder) }

  private fun report() = ProjectChecker(folder, git).check()

  private fun staged() = ProjectChecker(folder, git, staged = true).check().errors.map { it.file }

  private fun initialCommit() {
    git.require("init", "--quiet", "-b", "main")
    Files.writeString(folder.resolve("readme.txt"), "x")
    TestGit.commitAll(git, "base")
  }

  @Test
  fun `a binary file with spaces and non-ASCII letters is named whole`() {
    git.require("init", "--quiet")
    Files.write(folder.resolve("café hero.png"), byteArrayOf(0, 1, 2))

    val before = report().errors.single()
    Files.writeString(
      folder.resolve(".gitattributes"),
      "*.png filter=lfs diff=lfs merge=lfs -text\n",
    )

    assertEquals("café hero.png", before.file)
    assertEquals(emptyList(), report().errors)
  }

  @Test
  fun `a file git lists once per merge stage is reported once`() {
    initialCommit()
    git.require("checkout", "--quiet", "-b", "left")
    Files.write(folder.resolve("hero.png"), byteArrayOf(1))
    TestGit.commitAll(git, "left")
    git.require("checkout", "--quiet", "-b", "right", "main")
    Files.write(folder.resolve("hero.png"), byteArrayOf(2))
    TestGit.commitAll(git, "right")
    git.run("merge", "--no-edit", "left")

    val errors = report().errors.filter { it.kind == FindingKind.LFS_UNCOVERED }

    assertEquals(listOf("hero.png"), errors.map { it.file })
  }

  @Test
  fun `the staged check reads the index, not the files on disk`() {
    initialCommit()
    Files.write(folder.resolve("big.dat"), ByteArray(1_100_000))
    Files.write(folder.resolve("hero.png"), byteArrayOf(0, 1))
    git.require("add", "hero.png")

    assertEquals(listOf("hero.png"), staged())
    Files.writeString(folder.resolve(".gitattributes"), "*.png filter=lfs -text\n")
    assertEquals(listOf("hero.png"), staged())
    assertEquals(listOf("big.dat"), report().errors.map { it.file })
    git.require("add", ".gitattributes")
    assertEquals(emptyList(), staged())
  }

  @Test
  fun `the staged check finds conflict markers that are staged and not those only on disk`() {
    git.require("init", "--quiet", "-b", "main")
    val notes = folder.resolve("notes.txt")
    Files.writeString(notes, "<<<<<<< a\nx\n=======\ny\n>>>>>>> b\n")
    git.require("add", "notes.txt")

    assertEquals(listOf("notes.txt"), staged())
    Files.writeString(notes, "settled\n")
    assertEquals(listOf("notes.txt"), staged())
    git.require("add", "notes.txt")
    assertEquals(emptyList(), staged())
    Files.writeString(notes, "<<<<<<< a\nx\n=======\ny\n>>>>>>> b\n")
    assertEquals(emptyList(), staged())
  }
}

package dev.chieppa.ease.cli

import dev.chieppa.ease.cli.hook.GitInitHook
import dev.chieppa.ease.project.TestGit
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.io.TempDir

class GitProjectTest {
  @TempDir lateinit var work: Path

  private fun newGame(vararg extra: String): Harness {
    val output = CliOutput(System.out, System.err)
    val hook = GitInitHook(output, "/opt/ease/bin/ease") { TestGit.isolated(it) }
    val harness = Harness(work, listOf(hook))
    assertEquals(
      ExitCodes.OK,
      harness.run("new", "empty", "--name", "Moon Garden", *extra),
      harness.stderr,
    )
    return harness
  }

  @Test
  fun `a new game is a git repository with its first commit, attributes and merge driver`() {
    newGame()

    val game = work.resolve("moon-garden")
    val git = TestGit.isolated(game)
    assertEquals("Create Moon Garden", git.require("log", "-1", "--format=%s").trim())
    assertEquals("", git.require("status", "--porcelain").trim())
    assertTrue(game.resolve(".gitignore").readText().contains(".ease/cache/"))
    assertTrue(game.resolve(".gitattributes").readText().contains("*.png filter=lfs"))
    assertEquals(
      "'/opt/ease/bin/ease' merge-driver %O %A %B %P",
      git.require("config", "--local", "merge.ease.driver").trim(),
    )
    assertTrue(
      game.resolve(".git/hooks/pre-commit").readText().contains("exec \"\$ease_bin\" check")
    )
  }

  @Test
  fun `no-git leaves the game without a repository`() {
    newGame("--no-git")

    val game = work.resolve("moon-garden")
    assertTrue(game.resolve("settings.gradle.kts").exists())
    assertFalse(game.resolve(".git").exists())
    assertFalse(game.resolve(".gitattributes").exists())
  }

  @Test
  fun `a generated game passes ease check`() {
    newGame()
    val checking = Harness(work.resolve("moon-garden"))

    val status = checking.run("check")

    assertEquals(ExitCodes.OK, status, checking.stdout + checking.stderr)
    assertFalse(checking.stdout.contains("error "), checking.stdout)
  }
}

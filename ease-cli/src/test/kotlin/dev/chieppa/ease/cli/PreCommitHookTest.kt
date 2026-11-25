package dev.chieppa.ease.cli

import dev.chieppa.ease.cli.hook.GitInitHook
import dev.chieppa.ease.project.TestGit
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.io.TempDir

/** The hook `ease new` installs, run by real git with this command line behind it. */
class PreCommitHookTest {
  @TempDir lateinit var work: Path

  private val root by lazy { work.resolve("moon") }
  private val git by lazy { TestGit.isolated(root) }

  private fun createGame() {
    val launcher = TestLauncher.script(work)
    val hook = GitInitHook(CliOutput(System.out, System.err), launcher) { TestGit.isolated(it) }
    val created = Harness(work, listOf(hook))
    assertEquals(ExitCodes.OK, created.run("new", "empty", "--name", "Moon"), created.stderr)
  }

  @Test
  fun `an untracked big binary does not stop a commit and a staged one does`() {
    createGame()
    Files.write(root.resolve("huge.dat"), ByteArray(1_100_000))
    Files.writeString(root.resolve("notes.txt"), "notes")
    git.require("add", "notes.txt")

    val allowed = git.run("commit", "--quiet", "-m", "notes")

    assertEquals(0, allowed.exitCode, allowed.error + allowed.output)
    git.require("add", "huge.dat")
    Files.writeString(root.resolve("more.txt"), "more")
    git.require("add", "more.txt")

    val refused = git.run("commit", "--quiet", "-m", "huge")

    assertNotEquals(0, refused.exitCode)
    assertTrue((refused.output + refused.error).contains("lfs-uncovered"), refused.error)
    assertTrue((refused.output + refused.error).contains("huge.dat"), refused.error)
  }
}

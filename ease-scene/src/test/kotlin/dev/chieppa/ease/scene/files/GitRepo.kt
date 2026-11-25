package dev.chieppa.ease.scene.files

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals

/**
 * A git repository in a test folder, driven through the `git` command line. It ignores the
 * machine's own git configuration, so hooks, signing and line-ending settings there cannot change
 * what a test sees.
 */
class GitRepo(val root: Path) {
  private val config: Path = Files.createTempFile("ease-git", ".config")

  /** What one git command printed, and how it ended. */
  class Result(val exitCode: Int, val output: String)

  /** Runs `git` with [args] in the repository and returns how it went, without failing. */
  fun run(vararg args: String): Result {
    val command = listOf("git") + SETTINGS.flatMap { listOf("-c", it) } + args
    val process =
      ProcessBuilder(command).directory(root.toFile()).redirectErrorStream(true).apply {
        environment()["GIT_CONFIG_GLOBAL"] = config.toString()
        environment()["GIT_CONFIG_NOSYSTEM"] = "1"
        environment()["GIT_TERMINAL_PROMPT"] = "0"
      }
    val started = process.start()
    started.outputStream.close()
    val output = started.inputStream.readAllBytes().toString(Charsets.UTF_8)
    return Result(started.waitFor(), output)
  }

  /** Runs `git` with [args] and fails the test, showing git's output, unless it succeeds. */
  fun git(vararg args: String): String {
    val result = run(*args)
    assertEquals(0, result.exitCode, "git ${args.joinToString(" ")}:\n${result.output}")
    return result.output
  }

  /** Stages everything and commits it as [message]. */
  fun commitAll(message: String) {
    git("add", "--all")
    git("commit", "--quiet", "-m", message)
  }

  companion object {
    /** A new repository in [root] on the branch `main`. */
    fun init(root: Path): GitRepo = GitRepo(root).apply { git("init", "--quiet", "-b", "main") }

    private val SETTINGS =
      listOf(
        "user.name=Ease Test",
        "user.email=test@ease.invalid",
        "commit.gpgsign=false",
        "core.autocrlf=false",
        "core.hooksPath=.no-hooks",
      )
  }
}

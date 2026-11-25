package dev.chieppa.ease.project.git

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

/**
 * The `git` command line, run as a separate process in [directory], so the person's own config,
 * credentials and hooks apply and nothing of git is linked. Every call blocks until git ends.
 * [environment] is added to git's environment and [settings] are passed as `-c key=value`, which
 * lets a test run git apart from the machine's own configuration.
 */
public class Git(
  private val directory: Path,
  private val environment: Map<String, String> = emptyMap(),
  private val settings: List<String> = emptyList(),
) {
  /** Runs `git` with [args]; never throws for a failing command, only for a missing git. */
  public fun run(vararg args: String): GitResult = run(args.toList())

  /** Runs `git` with [args] and fails with git's message unless it succeeds. */
  public fun require(vararg args: String): String {
    val result = run(args.toList())
    if (!result.succeeded) {
      throw GitFailure(
        "git ${args.joinToString(" ")}: ${result.error.ifBlank { result.output }.trim()}"
      )
    }
    return result.output
  }

  /** Whether a `git` program can be started at all. */
  public fun isInstalled(): Boolean =
    try {
      run("--version").succeeded
    } catch (_: GitFailure) {
      false
    }

  /** Whether [directory] is inside a git repository's work tree. */
  public fun isRepository(): Boolean = isInstalled() && run("rev-parse", "--git-dir").succeeded

  /** Runs `git` with [args]; never throws for a failing command, only for a missing git. */
  public fun run(args: List<String>): GitResult {
    val command = listOf("git") + settings.flatMap { listOf("-c", it) } + args
    val errors = Files.createTempFile("ease-git", ".err")
    try {
      val builder =
        ProcessBuilder(command)
          .directory(directory.toFile())
          .redirectError(errors.toFile())
          .redirectInput(ProcessBuilder.Redirect.from(NULL_INPUT))
      builder.environment().putAll(environment)
      val process = builder.start()
      val output = process.inputStream.readAllBytes().toString(Charsets.UTF_8)
      val exit = process.waitFor()
      return GitResult(exit, output, Files.readString(errors))
    } catch (failure: IOException) {
      throw GitFailure("git could not be run: ${failure.message}", failure)
    } finally {
      Files.deleteIfExists(errors)
    }
  }

  private companion object {
    val NULL_INPUT: java.io.File =
      java.io.File(if (System.getProperty("os.name").startsWith("Windows")) "NUL" else "/dev/null")
  }
}

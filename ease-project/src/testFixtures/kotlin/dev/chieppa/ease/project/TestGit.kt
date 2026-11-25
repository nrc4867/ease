package dev.chieppa.ease.project

import dev.chieppa.ease.project.git.Git
import java.nio.file.Files
import java.nio.file.Path

/**
 * Git for tests: a [Git] that ignores the machine's own configuration, so identity, signing,
 * line-ending settings and templates there cannot change what a test sees.
 */
object TestGit {
  /** A git that runs in [directory] with a configuration of its own. */
  fun isolated(directory: Path): Git {
    val config = Files.createTempFile("ease-git", ".config")
    return Git(
      directory,
      mapOf(
        "GIT_CONFIG_GLOBAL" to config.toString(),
        "GIT_CONFIG_NOSYSTEM" to "1",
        "GIT_TERMINAL_PROMPT" to "0",
      ),
      listOf(
        "user.name=Ease Test",
        "user.email=test@ease.invalid",
        "commit.gpgsign=false",
        "core.autocrlf=false",
        "init.defaultBranch=main",
      ),
    )
  }

  /** Stages everything in the repository [git] works in and commits it as [message]. */
  fun commitAll(git: Git, message: String) {
    git.require("add", "--all")
    git.require("commit", "--quiet", "--no-verify", "-m", message)
  }
}

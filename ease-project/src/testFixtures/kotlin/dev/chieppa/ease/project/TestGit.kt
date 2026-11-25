package dev.chieppa.ease.project

import dev.chieppa.ease.project.git.Git
import java.nio.file.Files
import java.nio.file.Path

/**
 * Git for tests: a [Git] that ignores the machine's own configuration, so identity, signing,
 * line-ending settings and templates there cannot change what a test sees.
 */
object TestGit {
  private val INSTANT =
    mapOf(
      "GIT_AUTHOR_DATE" to "2001-02-03T04:05:06+00:00",
      "GIT_COMMITTER_DATE" to "2001-02-03T04:05:06+00:00",
    )

  /**
   * A git that runs in [directory] with a configuration of its own. With [oneInstant] every commit
   * it makes has the same date, as commits made within one second do.
   */
  fun isolated(directory: Path, oneInstant: Boolean = false): Git {
    val config = Files.createTempFile("ease-git", ".config")
    return Git(
      directory,
      mapOf(
        "GIT_CONFIG_GLOBAL" to config.toString(),
        "GIT_CONFIG_NOSYSTEM" to "1",
        "GIT_TERMINAL_PROMPT" to "0",
      ) + if (oneInstant) INSTANT else emptyMap(),
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

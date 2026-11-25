package dev.chieppa.ease.project.git

import dev.chieppa.ease.project.layout.ProjectLayout
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions

/** What [GitSetup] did, and the things a person should know about. */
public class GitSetupResult(
  /** Whether the first commit was made. */
  public val committed: Boolean,
  /** Sentences about what could not be done, such as LFS not being installed. */
  public val notes: List<String>,
)

/**
 * Puts a new project folder in git: `git init`, `.gitignore` and `.gitattributes`, Git LFS, Ease's
 * merge driver in the local config, the first commit and then the pre-commit hook. [easeCommand] is
 * the `ease` program the merge driver and the hook run.
 */
public class GitSetup(private val git: Git, private val easeCommand: String) {
  /**
   * Sets up the repository in the folder [git] works in, committing everything in it as [message].
   * Throws [GitFailure] when git is not installed or cannot start the repository.
   */
  public fun apply(project: Path, message: String): GitSetupResult {
    if (!git.isInstalled()) throw GitFailure("git is not installed; use --no-git to skip it")
    val notes = mutableListOf<String>()
    git.require("init", "--quiet")
    git.require("symbolic-ref", "HEAD", "refs/heads/$BRANCH")
    Files.writeString(project.resolve(GitIgnoreFile.NAME), GitIgnoreFile.text())
    Files.writeString(project.resolve(GitAttributesFile.NAME), GitAttributesFile.text())
    registerMergeDriver()
    if (!git.run("lfs", "install", "--local").succeeded) {
      notes +=
        "Git LFS is not installed: binary assets stay plain files until it is (see git-lfs.com)"
    }
    val committed = commit(message, notes)
    writeHook(project)
    return GitSetupResult(committed, notes)
  }

  private fun registerMergeDriver() {
    val key = "merge.${GitAttributesFile.MERGE_DRIVER}"
    git.require("config", "--local", "$key.name", "Ease scene and prefab files")
    git.require(
      "config",
      "--local",
      "$key.driver",
      "${ShellQuote.quote(easeCommand)} merge-driver %O %A %B %P",
    )
  }

  private fun commit(message: String, notes: MutableList<String>): Boolean {
    git.require("add", "--all")
    val result = git.run("commit", "--quiet", "-m", message)
    if (!result.succeeded) {
      notes += "No first commit was made: ${result.error.ifBlank { result.output }.trim()}"
    }
    return result.succeeded
  }

  private fun writeHook(project: Path) {
    val hook = project.resolve(ProjectLayout.GIT_FOLDER).resolve(PreCommitHook.PATH)
    Files.createDirectories(hook.parent)
    Files.writeString(hook, PreCommitHook.script(easeCommand))
    try {
      Files.setPosixFilePermissions(hook, PosixFilePermissions.fromString(EXECUTABLE))
    } catch (_: UnsupportedOperationException) {
      return
    } catch (_: IOException) {
      return
    }
  }

  private companion object {
    const val BRANCH = "main"
    const val EXECUTABLE = "rwxr-xr-x"
  }
}

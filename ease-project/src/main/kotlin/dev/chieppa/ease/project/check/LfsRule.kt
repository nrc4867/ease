package dev.chieppa.ease.project.check

import dev.chieppa.ease.project.git.BinaryAssets
import dev.chieppa.ease.project.git.Git
import dev.chieppa.ease.project.git.GitAttributesFile
import dev.chieppa.ease.project.git.GitFailure
import dev.chieppa.ease.project.layout.ProjectLayout
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

/**
 * Every binary asset is in Git LFS: a file with a binary asset extension, or any other binary file
 * over [BinaryAssets.MAX_PLAIN_BYTES], must match an LFS rule in `.gitattributes`. Needs a git
 * repository; says so, as a warning, when Git LFS itself is not installed, and checks the rest.
 *
 * Looks at the working tree, or with [staged] at the index: the files and rules about to be
 * committed, and, for the size rule, only the files this commit changes.
 */
internal class LfsRule(
  private val project: Path,
  private val git: Git,
  private val staged: Boolean = false,
) {
  fun findings(): List<Finding> {
    if (!ProjectLayout.isRepository(project)) return emptyList()
    return try {
      if (!git.isInstalled()) emptyList() else coverage() + installation()
    } catch (_: GitFailure) {
      emptyList()
    }
  }

  private fun installation(): List<Finding> =
    if (git.run("lfs", "version").succeeded) {
      emptyList()
    } else {
      listOf(
        Finding(
          Severity.WARNING,
          FindingKind.LFS_NOT_INSTALLED,
          GitAttributesFile.NAME,
          "Git LFS is not installed, so binary assets are committed as plain files; install " +
            "git-lfs (git-lfs.com) and run git lfs install",
        )
      )
    }

  private fun coverage(): List<Finding> {
    val candidates = binaryFiles()
    val covered = candidates.chunked(BATCH).flatMap { lfsTracked(it) }.toSet()
    return candidates
      .filter { it !in covered }
      .map {
        Finding(
          Severity.ERROR,
          FindingKind.LFS_UNCOVERED,
          it,
          "is a binary file that Git LFS does not track; run git lfs track for its type, then " +
            "git add --renormalize",
        )
      }
  }

  /** The binary assets git knows, once each, as paths from the repository root. */
  private fun binaryFiles(): List<String> {
    val listed =
      if (staged) git.run("ls-files", "-z", "--cached")
      else git.run("ls-files", "-z", "--cached", "--others", "--exclude-standard")
    val largeStaged = if (staged) largeStagedBinaries() else emptySet()
    val paths = listed.nulFields.distinct()
    return paths.filter { isBinaryAsset(it, largeStaged) }.sorted()
  }

  private fun isBinaryAsset(path: String, largeStaged: Set<String>): Boolean =
    BinaryAssets.hasBinaryExtension(path) ||
      if (staged) path in largeStaged else isLargeBinaryFile(project.resolve(path))

  private fun isLargeBinaryFile(file: Path): Boolean =
    try {
      Files.isRegularFile(file) &&
        Files.size(file) > BinaryAssets.MAX_PLAIN_BYTES &&
        ProjectFiles.isBinary(file)
    } catch (_: IOException) {
      false
    }

  /** The staged binary files over the size limit that this commit adds or changes. */
  private fun largeStagedBinaries(): Set<String> {
    val numstat = git.run("diff", "--cached", "--numstat", "--no-renames", "-z")
    val binaries = numstat.nulFields.filter { it.startsWith(BINARY_COUNTS) }
    return binaries.map { it.removePrefix(BINARY_COUNTS) }.filter { stagedSize(it) > LIMIT }.toSet()
  }

  private fun stagedSize(path: String): Long =
    git.run("cat-file", "-s", ":$path").output.let { it.trim().toLongOrNull() ?: 0L }

  /** The paths among [paths] whose `filter` attribute is `lfs`. */
  private fun lfsTracked(paths: List<String>): List<String> {
    val cached = if (staged) listOf("--cached") else emptyList()
    val result = git.run(listOf("check-attr") + cached + listOf("-z", "filter", "--") + paths)
    val attributes = result.nulFields.chunked(ATTRIBUTE_FIELDS)
    return attributes.filter { it.last() == LFS }.map { it.first() }
  }

  private companion object {
    const val BATCH = 50
    const val LFS = "lfs"
    const val ATTRIBUTE_FIELDS = 3
    const val BINARY_COUNTS = "-\t-\t"
    const val LIMIT = BinaryAssets.MAX_PLAIN_BYTES
  }
}

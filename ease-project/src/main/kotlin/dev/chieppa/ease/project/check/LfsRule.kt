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
 */
internal class LfsRule(private val project: Path, private val git: Git) {
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

  /** The files git knows or would add that are binary assets, as paths from the repository root. */
  private fun binaryFiles(): List<String> {
    val listed = git.run("ls-files", "-z", "--cached", "--others", "--exclude-standard")
    val paths = listed.output.split(NUL).filter { it.isNotEmpty() }
    return paths.filter(::isBinaryAsset).sorted()
  }

  private fun isBinaryAsset(path: String): Boolean {
    if (BinaryAssets.hasBinaryExtension(path)) return true
    val file = project.resolve(path)
    return try {
      Files.isRegularFile(file) &&
        Files.size(file) > BinaryAssets.MAX_PLAIN_BYTES &&
        ProjectFiles.isBinary(file)
    } catch (_: IOException) {
      false
    }
  }

  private fun lfsTracked(paths: List<String>): List<String> {
    val result = git.run(listOf("check-attr", "filter", "--") + paths)
    return result.lines.mapNotNull { line ->
      line.substringBefore(ATTRIBUTE_SEPARATOR).takeIf { line.endsWith(": lfs") }
    }
  }

  private companion object {
    const val NUL = '\u0000'
    const val BATCH = 50
    const val ATTRIBUTE_SEPARATOR = ": filter: "
  }
}

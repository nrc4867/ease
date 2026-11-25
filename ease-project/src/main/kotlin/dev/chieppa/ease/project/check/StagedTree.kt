package dev.chieppa.ease.project.check

import dev.chieppa.ease.platform.files.FileTrees
import dev.chieppa.ease.project.git.BinaryAssets
import dev.chieppa.ease.project.git.Git
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

/**
 * The files in git's index, written to a folder of their own, so the rules that read a project
 * folder can look at what is about to be committed instead of what is on disk. A file with a binary
 * asset extension is an empty placeholder, and Git LFS files are not downloaded. [close] deletes
 * the folder.
 */
internal class StagedTree private constructor(val root: Path) : AutoCloseable {
  override fun close() {
    FileTrees.deleteRecursively(root)
  }

  companion object {
    private const val BATCH = 200

    /** Writes the index of the repository [git] works in to a new temporary folder. */
    fun of(git: Git): StagedTree {
      val tree = StagedTree(Files.createTempDirectory("ease-staged"))
      val paths = git.run("ls-files", "-z", "--cached").nulFields.distinct()
      val (placeholders, texts) = paths.partition(BinaryAssets::hasBinaryExtension)
      placeholders.forEach { path ->
        val file = tree.root.resolve(path)
        Files.createDirectories(file.parent)
        Files.createFile(file)
      }
      val prefix = tree.root.toString().replace(File.separatorChar, '/') + "/"
      val plain = git.withEnvironment(mapOf("GIT_LFS_SKIP_SMUDGE" to "1"))
      texts.chunked(BATCH).forEach {
        plain.run(listOf("checkout-index", "-f", "--prefix=$prefix", "--") + it)
      }
      return tree
    }
  }
}

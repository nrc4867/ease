package dev.chieppa.ease.project.check

import dev.chieppa.ease.project.layout.ProjectLayout
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

/** The files of a project folder that checks read. */
public object ProjectFiles {
  /** Folders nothing of the project's own is in: git's, build output, tool caches. */
  public val SKIPPED_FOLDERS: Set<String> =
    setOf(
      ProjectLayout.GIT_FOLDER,
      ProjectLayout.EASE_FOLDER,
      ".gradle",
      ".kotlin",
      ".idea",
      "build",
      "out",
      "node_modules",
    )

  /** The longest text file read for conflict markers, in bytes. */
  public const val MAX_TEXT_BYTES: Long = 8_388_608

  private const val SNIFF_BYTES = 8_000

  /** Every regular file under [project] outside [SKIPPED_FOLDERS], in path order. */
  public fun all(project: Path): List<Path> {
    val found = mutableListOf<Path>()
    Files.walkFileTree(
      project,
      object : SimpleFileVisitor<Path>() {
        override fun preVisitDirectory(
          dir: Path,
          attrs: BasicFileAttributes,
        ) =
          if (dir != project && dir.fileName.toString() in SKIPPED_FOLDERS)
            FileVisitResult.SKIP_SUBTREE
          else FileVisitResult.CONTINUE

        override fun visitFile(
          file: Path,
          attrs: BasicFileAttributes,
        ): FileVisitResult {
          if (attrs.isRegularFile) found.add(file)
          return FileVisitResult.CONTINUE
        }
      },
    )
    return found.sorted()
  }

  /** The text of [file], or null when it is binary, too large or cannot be read. */
  public fun textOf(file: Path): String? =
    try {
      if (Files.size(file) > MAX_TEXT_BYTES || isBinary(file)) null else Files.readString(file)
    } catch (_: IOException) {
      null
    }

  /** Whether the start of [file] holds a zero byte, as binary files do. */
  public fun isBinary(file: Path): Boolean =
    Files.newInputStream(file).use { stream ->
      stream.readNBytes(SNIFF_BYTES).any { it == 0.toByte() }
    }
}

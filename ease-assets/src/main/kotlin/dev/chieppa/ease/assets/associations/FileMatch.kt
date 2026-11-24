package dev.chieppa.ease.assets.associations

import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.PathMatcher

private const val EXTENSION_TIER = 1_000
private const val GLOB_TIER = 2_000
private const val SNIFFER_TIER = 3_000
private val WILDCARDS = setOf('*', '?', '[', ']', '{', '}')

/**
 * What a contributor registers for: which files it applies to, and how specifically. When several
 * contributors match one file, the higher [specificity] wins before priority is compared with the
 * rest: a content sniffer beats a glob, which beats a bare extension, and within a kind the longer
 * literal text beats the shorter.
 */
public sealed interface FileMatch {
  /** Higher matches narrower sets of files. */
  public val specificity: Int

  /** Whether this match applies to [file]. */
  public fun matches(file: ProjectFile): Boolean

  /** Files whose name ends in [extension], such as `png` or the compound `geo.json`. */
  public class Extension(extension: String) : FileMatch {
    private val suffix = "." + extension.removePrefix(".").lowercase()

    override val specificity: Int = EXTENSION_TIER + suffix.length

    override fun matches(file: ProjectFile): Boolean = file.name.lowercase().endsWith(suffix)

    override fun toString(): String = suffix
  }

  /**
   * Files matching [pattern], a glob. A pattern without a slash is tried on the file's name, one
   * with a slash on its path inside the project.
   */
  public class Glob(public val pattern: String) : FileMatch {
    private val matcher: PathMatcher = FileSystems.getDefault().getPathMatcher("glob:$pattern")
    private val onName = '/' !in pattern

    override val specificity: Int = GLOB_TIER + pattern.count { it !in WILDCARDS }

    override fun matches(file: ProjectFile): Boolean {
      val target: Path = if (onName) file.path.fileName else file.path
      return matcher.matches(target)
    }

    override fun toString(): String = pattern
  }

  /**
   * Files with [extension] whose first bytes satisfy [accepts], for formats that share an
   * extension, such as the several kinds of `.json`.
   */
  public class Sniffer(extension: String, private val accepts: (ByteArray) -> Boolean) : FileMatch {
    private val extensionMatch = Extension(extension)

    override val specificity: Int = SNIFFER_TIER + extensionMatch.specificity - EXTENSION_TIER

    override fun matches(file: ProjectFile): Boolean =
      extensionMatch.matches(file) && accepts(file.head)

    override fun toString(): String = "$extensionMatch by content"
  }
}

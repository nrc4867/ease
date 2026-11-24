package dev.chieppa.ease.assets.associations

import java.nio.file.Files
import java.nio.file.Path

private const val SNIFF_BYTES = 512

/**
 * A file of the project as contributors are matched against it: its path inside the project, and
 * its first bytes, read only when a content sniffer asks.
 */
public class ProjectFile(public val path: Path, private val readHead: () -> ByteArray) {
  /** The file's name with its extensions, such as `rock.geo.json`. */
  public val name: String = path.fileName.toString()

  /** The first bytes of the file, enough to tell formats that share an extension apart. */
  public val head: ByteArray by lazy(readHead)

  override fun toString(): String = path.toString()

  public companion object {
    /** The file at [relative] under the project folder [root]. */
    public fun under(root: Path, relative: Path): ProjectFile =
      ProjectFile(relative) {
        Files.newInputStream(root.resolve(relative)).use { it.readNBytes(SNIFF_BYTES) }
      }
  }
}

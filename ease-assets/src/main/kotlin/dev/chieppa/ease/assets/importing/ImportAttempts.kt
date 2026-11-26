package dev.chieppa.ease.assets.importing

import dev.chieppa.ease.assets.Guid
import dev.chieppa.ease.assets.database.CacheFolder
import java.nio.file.Files
import java.nio.file.Path

private const val ATTEMPTED_FILE = "attempted"

/**
 * The files each source's latest failed import asked for, in an `attempted` file beside its cached
 * results under the content folder [root], so a file that appears or changes later can import the
 * source again. A good import forgets them.
 */
internal class ImportAttempts(private val root: Path) {
  /** Records that [guid]'s import, from [source], failed after asking for the files [attempted]. */
  fun failed(guid: Guid, source: Path, attempted: Collection<String>) {
    CacheFolder.ensure(root)
    val folder = importFolder(root, guid)
    Files.createDirectories(folder)
    Files.writeString(folder.resolve(SOURCE_FILE), source.joinToString("/"))
    Files.write(folder.resolve(ATTEMPTED_FILE), attempted)
  }

  /** Forgets [guid]'s failed import, after a good one. */
  fun succeeded(guid: Guid) {
    Files.deleteIfExists(importFolder(root, guid).resolve(ATTEMPTED_FILE))
  }

  /** The files [guid]'s latest import asked for when it failed; empty after a good import. */
  fun of(guid: Guid): List<String> {
    val file = importFolder(root, guid).resolve(ATTEMPTED_FILE)
    return if (Files.isRegularFile(file)) Files.readAllLines(file).filter { it.isNotEmpty() }
    else emptyList()
  }
}

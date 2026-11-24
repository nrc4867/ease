package dev.chieppa.ease.assets.database

import dev.chieppa.ease.assets.ContentHash
import dev.chieppa.ease.assets.meta.MetaFileException
import dev.chieppa.ease.assets.meta.MetaFiles
import java.nio.file.Files
import java.nio.file.Path

/**
 * Pairs meta files that lost their source with new source files that have no meta file, by content
 * hash: the file was moved or renamed without its meta file, and moving the meta file after it
 * keeps the GUID.
 */
internal class OrphanMatcher(private val root: Path) {
  /**
   * Moves each orphaned meta file in [problems] beside the one source file it matches, and returns
   * the source paths whose state changed (the new sources and the old ones).
   */
  fun match(problems: Map<Path, AssetProblem>): Set<Path> {
    val orphans = problems.filterValues { it is AssetProblem.MetaWithoutSource }.keys
    val unclaimed =
      problems.filterValues { it is AssetProblem.SourceWithoutMeta }.keys.toMutableList()
    if (orphans.isEmpty() || unclaimed.isEmpty()) return emptySet()
    val hashes = unclaimed.associateWith { ContentHash.of(root.resolve(it)) }
    val touched = LinkedHashSet<Path>()
    for (orphan in orphans) {
      val target = matchFor(orphan, unclaimed, hashes) ?: continue
      Files.move(MetaFiles.metaOf(root.resolve(orphan)), MetaFiles.metaOf(root.resolve(target)))
      unclaimed.remove(target)
      touched.add(orphan)
      touched.add(target)
    }
    return touched
  }

  /** The one source in [unclaimed] whose hash is the one [orphan]'s meta file recorded. */
  private fun matchFor(orphan: Path, unclaimed: List<Path>, hashes: Map<Path, String>): Path? {
    val hash =
      try {
        MetaFiles.read(MetaFiles.metaOf(root.resolve(orphan))).contentHash
      } catch (_: MetaFileException) {
        return null
      }
    return unclaimed.singleOrNull { hashes[it] == hash }
  }
}

package dev.chieppa.ease.assets.database

import dev.chieppa.ease.assets.meta.MetaFiles
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

/**
 * Pairs meta files that lost their source with new source files that have no meta file, by content
 * hash: the file was moved or renamed without its meta file, and moving the meta file after it
 * keeps the GUID. The orphan's hash is the one the [cache] last recorded for its old path, so with
 * no cache there is no match.
 */
internal class OrphanMatcher(
  private val root: Path,
  private val cache: HashCache,
  private val sources: SourceFiles,
) {
  /**
   * Moves each orphaned meta file in [problems] beside the one source file it matches, and returns
   * the source paths whose state changed (the new sources and the old ones). Paths that are not on
   * disk are skipped.
   */
  fun match(problems: Map<Path, AssetProblem>): Set<Path> {
    val orphans =
      existing(problems, AssetProblem.MetaWithoutSource::class.java) {
        Files.isRegularFile(metaOf(it))
      }
    val unclaimed =
      existing(problems, AssetProblem.SourceWithoutMeta::class.java) { sources.exists(it) }
        .toMutableList()
    if (orphans.isEmpty() || unclaimed.isEmpty()) return emptySet()
    val hashes = unclaimed.mapNotNull { path -> hashOf(path)?.let { path to it } }.toMap()
    val touched = LinkedHashSet<Path>()
    for (orphan in orphans) {
      val target =
        matchFor(orphan, hashes.filterKeys { it in unclaimed })?.takeIf { move(orphan, it) }
          ?: continue
      unclaimed.remove(target)
      cache.remove(orphan)
      touched.add(orphan)
      touched.add(target)
    }
    return touched
  }

  private fun metaOf(source: Path): Path = MetaFiles.metaOf(root.resolve(source))

  /** The keys of [problems] of type [type] whose file is still on disk, as [onDisk] says. */
  private fun <T : AssetProblem> existing(
    problems: Map<Path, AssetProblem>,
    type: Class<T>,
    onDisk: (Path) -> Boolean,
  ): List<Path> =
    problems.entries.filter { type.isInstance(it.value) && onDisk(it.key) }.map { it.key }

  /** The one source among [hashes] whose hash is the one the cache recorded for [orphan]. */
  private fun matchFor(orphan: Path, hashes: Map<Path, String>): Path? {
    val recorded = cache[orphan] ?: return null
    return hashes.entries.singleOrNull { it.value == recorded }?.key
  }

  private fun hashOf(source: Path): String? =
    try {
      sources.hash(source)
    } catch (_: IOException) {
      null
    }

  private fun move(orphan: Path, target: Path): Boolean =
    try {
      Files.move(metaOf(orphan), metaOf(target))
      true
    } catch (_: IOException) {
      false
    }
}

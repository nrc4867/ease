package dev.chieppa.ease.assets.database

import dev.chieppa.ease.assets.Guid
import dev.chieppa.ease.assets.meta.AssetMeta
import dev.chieppa.ease.assets.meta.MetaFileException
import dev.chieppa.ease.assets.meta.MetaFiles
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

/**
 * Brings the index and the problems in line with what is on disk for one source path: the single
 * rule behind loading a project and applying a change. A file that cannot be read becomes a
 * problem, never an exception, and a GUID given up by one path is remembered so the paths that were
 * refused it can be checked again.
 */
internal class AssetRefresher(
  private val root: Path,
  private val index: AssetIndex,
  private val problems: MutableMap<Path, AssetProblem>,
  private val cache: HashCache,
  private val sources: SourceFiles,
) {
  private val released = LinkedHashSet<Guid>()

  /** Reads [source] (relative) from disk, records what changed in [changes]. */
  fun refresh(source: Path, changes: MutableList<AssetChange>) {
    val absolute = root.resolve(source)
    val metaPath = MetaFiles.metaOf(absolute)
    val hasSource = sources.exists(source)
    val hasMeta = Files.isRegularFile(metaPath)
    problems.remove(source)
    when {
      hasSource && hasMeta -> refreshPair(source, metaPath, changes)
      hasSource -> {
        forget(source, changes)
        problems[source] = AssetProblem.SourceWithoutMeta(source)
      }
      hasMeta -> {
        forget(source, changes)
        problems[source] = AssetProblem.MetaWithoutSource(MetaFiles.metaOf(source))
      }
      else -> forget(source, changes)
    }
  }

  /**
   * The paths refused a GUID that has since been given up, so they can be refreshed: the ones whose
   * duplicate holder was deleted or changed its GUID.
   */
  fun takeRecheck(): List<Path> {
    val paths =
      problems.entries
        .filter { (it.value as? AssetProblem.DuplicateGuid)?.guid in released }
        .map { it.key }
    released.clear()
    return paths
  }

  private fun forget(source: Path, changes: MutableList<AssetChange>) {
    val old = index.at(source) ?: return
    index.remove(old)
    released.add(old.guid)
    changes.add(AssetChange.Removed(old.guid, old.path))
  }

  private fun refreshPair(
    source: Path,
    metaPath: Path,
    changes: MutableList<AssetChange>,
  ) {
    val meta = readMeta(source, metaPath, changes)
    val hash = if (meta == null) null else hashOf(source)
    if (meta != null && hash != null && claim(source, meta, changes)) {
      record(AssetRecord(meta.guid, source, hash), changes)
    }
  }

  private fun record(record: AssetRecord, changes: MutableList<AssetChange>) {
    val old = index.at(record.path)
    if (old != null && old.guid != record.guid) forget(record.path, changes)
    val previous = index.at(record.path)
    when {
      previous == null -> changes.add(AssetChange.Added(record))
      previous.contentHash != record.contentHash -> changes.add(AssetChange.Changed(record))
    }
    index.put(record)
    cache.put(record.path, record.contentHash)
  }

  /** The meta file's contents, or null after recording why it cannot be read. */
  private fun readMeta(
    source: Path,
    metaPath: Path,
    changes: MutableList<AssetChange>,
  ): AssetMeta? =
    try {
      MetaFiles.read(metaPath)
    } catch (failure: MetaFileException) {
      forget(source, changes)
      problems[source] =
        AssetProblem.UnreadableMeta(MetaFiles.metaOf(source), failure.message.orEmpty())
      null
    }

  /** The hash of the source, or null after recording that it cannot be read now. */
  private fun hashOf(source: Path): String? =
    try {
      sources.hash(source)
    } catch (failure: IOException) {
      problems[source] = AssetProblem.UnreadableSource(source, failure.message.orEmpty())
      null
    }

  /**
   * Whether [source] may hold [meta]'s GUID: false, with a problem recorded, when another source
   * still holds it. An asset whose old source is gone is released to move here.
   */
  private fun claim(source: Path, meta: AssetMeta, changes: MutableList<AssetChange>): Boolean {
    val other = index[meta.guid]?.takeIf { it.path != source }
    val holder = other?.takeIf { sources.exists(it.path) }
    if (holder == null) {
      other?.let { forget(it.path, changes) }
      return true
    }
    forget(source, changes)
    problems[source] = AssetProblem.DuplicateGuid(meta.guid, listOf(holder.path, source))
    return false
  }
}

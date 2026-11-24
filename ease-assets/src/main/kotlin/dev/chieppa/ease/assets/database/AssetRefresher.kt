package dev.chieppa.ease.assets.database

import dev.chieppa.ease.assets.ContentHash
import dev.chieppa.ease.assets.meta.AssetMeta
import dev.chieppa.ease.assets.meta.MetaFileException
import dev.chieppa.ease.assets.meta.MetaFiles
import java.nio.file.Files
import java.nio.file.Path

/**
 * Brings the index and the problems in line with what is on disk for one source path: the single
 * rule behind loading a project and applying a change. With [updateMeta], a meta file whose hash
 * lags its source is rewritten; loading leaves files alone.
 */
internal class AssetRefresher(
  private val root: Path,
  private val index: AssetIndex,
  private val problems: MutableMap<Path, AssetProblem>,
  private val updateMeta: Boolean,
) {
  /** Reads [source] (relative) from disk, records what changed in [changes]. */
  fun refresh(source: Path, changes: MutableList<AssetChange>) {
    val absolute = root.resolve(source)
    val metaPath = MetaFiles.metaOf(absolute)
    val hasSource = Files.isRegularFile(absolute)
    val hasMeta = Files.isRegularFile(metaPath)
    problems.remove(source)
    when {
      hasSource && hasMeta -> refreshPair(source, absolute, metaPath, changes)
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

  private fun forget(source: Path, changes: MutableList<AssetChange>) {
    val old = index.at(source) ?: return
    index.remove(old)
    changes.add(AssetChange.Removed(old.guid, old.path))
  }

  private fun refreshPair(
    source: Path,
    absolute: Path,
    metaPath: Path,
    changes: MutableList<AssetChange>,
  ) {
    val meta = readMeta(source, metaPath, changes)?.takeIf { claim(source, it, changes) } ?: return
    val old = index.at(source)
    if (old != null && old.guid != meta.guid) forget(source, changes)
    val hash = ContentHash.of(absolute)
    val record = AssetRecord(meta.guid, source, hash)
    val previous = index.at(source)
    when {
      previous == null -> changes.add(AssetChange.Added(record))
      previous.contentHash != hash -> changes.add(AssetChange.Changed(record))
      else -> return
    }
    index.put(record)
    if (updateMeta && meta.contentHash != hash) {
      MetaFiles.write(metaPath, AssetMeta(guid = meta.guid, contentHash = hash))
    }
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

  /**
   * Whether [source] may hold [meta]'s GUID: false, with a problem recorded, when another source
   * still holds it. An asset whose old source is gone is released to move here.
   */
  private fun claim(source: Path, meta: AssetMeta, changes: MutableList<AssetChange>): Boolean {
    val other = index[meta.guid]?.takeIf { it.path != source }
    val holder = other?.takeIf { Files.isRegularFile(root.resolve(it.path)) }
    if (holder == null) {
      other?.let { forget(it.path, changes) }
      return true
    }
    forget(source, changes)
    problems[source] = AssetProblem.DuplicateGuid(meta.guid, listOf(holder.path, source))
    return false
  }
}

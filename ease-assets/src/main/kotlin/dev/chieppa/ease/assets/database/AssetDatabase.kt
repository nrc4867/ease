package dev.chieppa.ease.assets.database

import dev.chieppa.ease.assets.Guid
import dev.chieppa.ease.assets.GuidSource
import dev.chieppa.ease.assets.meta.AssetMeta
import dev.chieppa.ease.assets.meta.MetaFiles
import dev.chieppa.ease.platform.files.FileWatcher
import java.nio.file.Files
import java.nio.file.Path

/**
 * The index of a project's assets: every source file under [root] with its meta file's GUID.
 * Lookups by GUID and by path are allocation-free. A watcher feeds it: [sync] applies what changed
 * on disk since the last call and returns it, so the owner decides when changes land, such as at a
 * tick boundary. When the watcher reports lost events, [sync] looks at every file again.
 *
 * Content hashes are kept per machine under `.ease/cache/` in [root], in a folder that ignores
 * itself in git, never in the meta files. A meta file whose source is gone finds the file it moved
 * to through the hash the cache recorded for its old path.
 *
 * Loading rejects a project with a GUID used twice, a meta file without a source (unless a new file
 * with the recorded hash takes it over) or a source without a meta file. After loading, such files
 * are reported in [problems] instead of stopping the running game.
 */
public class AssetDatabase
private constructor(
  /** The project's content folder; every record's path is relative to it. */
  public val root: Path,
  private val watcher: FileWatcher,
  private val guids: GuidSource,
) {
  private val index = AssetIndex()
  private val problemsByPath = LinkedHashMap<Path, AssetProblem>()
  private val collector = ChangeCollector(root)
  private val cache = HashCache(root)
  private val orphans = OrphanMatcher(root, cache)
  private val refresher = AssetRefresher(root, index, problemsByPath, cache)

  /** Every asset, in no particular order. */
  public val assets: Collection<AssetRecord>
    get() = index.records

  /** What is wrong with the project's files right now. */
  public val problems: List<AssetProblem>
    get() = problemsByPath.values.toList()

  /** The asset with [guid], or null when there is none. */
  public operator fun get(guid: Guid): AssetRecord? = index[guid]

  /** The asset whose source is at [path], relative to [root], or null when there is none. */
  public fun at(path: Path): AssetRecord? = index.at(path)

  /**
   * Applies the file changes collected since the last sync, then returns what happened to the
   * assets. Returns an empty list without allocating when nothing changed.
   */
  public fun sync(): List<AssetChange> {
    watcher.drain(collector)
    if (collector.isEmpty) return emptyList()
    val changes = mutableListOf<AssetChange>()
    val everything = collector.needsRescan()
    val paths = collector.take()
    settle(if (everything) allPaths() else paths.flatMap { withContents(it) }, changes)
    settle(orphans.match(problemsByPath), changes)
    cache.save()
    return pairMoves(changes)
  }

  /**
   * Gives the source file at [path] (relative to [root]) a meta file with a new GUID, as importing
   * a new file does. Throws [IllegalStateException] when the file is missing or has a meta file.
   */
  public fun adopt(path: Path): AssetRecord {
    val absolute = root.resolve(path)
    check(Files.isRegularFile(absolute)) { "No source file at $path to adopt" }
    val meta = MetaFiles.metaOf(absolute)
    check(!Files.exists(meta)) { "$path already has a meta file" }
    MetaFiles.write(meta, AssetMeta(guid = guids.next()))
    settle(listOf(path), mutableListOf())
    cache.save()
    return checkNotNull(index.at(path)) { "$path was not indexed after adopting" }
  }

  /** Refreshes [paths], then every path that was refused a GUID someone has given up. */
  private fun settle(paths: Collection<Path>, changes: MutableList<AssetChange>) {
    paths.distinct().forEach { refresher.refresh(it, changes) }
    var again = refresher.takeRecheck()
    while (again.isNotEmpty()) {
      again.forEach { refresher.refresh(it, changes) }
      again = refresher.takeRecheck()
    }
  }

  /** [source], plus everything the database knows under it when it is not a file. */
  private fun withContents(source: Path): List<Path> =
    if (Files.isRegularFile(root.resolve(source))) {
      listOf(source)
    } else {
      listOf(source) +
        index.under(source).map { it.path } +
        problemsByPath.keys.filter { it.startsWith(source) }
    }

  /** Every source on disk and every path the database holds, for a full look. */
  private fun allPaths(): List<Path> =
    sourcesOnDisk() + index.records.map { it.path } + problemsByPath.keys

  private fun sourcesOnDisk(): List<Path> =
    Files.walk(root).use { walk ->
      walk
        .filter { Files.isRegularFile(it) }
        .map { root.relativize(it) }
        .filter { !ProjectPaths.isHidden(it) }
        .map { if (MetaFiles.isMeta(it)) MetaFiles.sourceOf(it) else it }
        .distinct()
        .toList()
    }

  /** A removal and an addition of the same GUID in one sync is one move. */
  private fun pairMoves(changes: List<AssetChange>): List<AssetChange> {
    val removed = changes.filterIsInstance<AssetChange.Removed>().associateBy { it.guid }
    val moved =
      changes.filterIsInstance<AssetChange.Added>().map { it.guid }.filter { it in removed }
    return changes.mapNotNull { change ->
      when {
        change is AssetChange.Added && change.guid in moved ->
          AssetChange.Moved(change.record, removed.getValue(change.guid).path)
        change is AssetChange.Removed && change.guid in moved -> null
        else -> change
      }
    }
  }

  private fun scan() {
    settle(sourcesOnDisk(), mutableListOf())
    settle(orphans.match(problemsByPath), mutableListOf())
    cache.retain { Files.exists(MetaFiles.metaOf(root.resolve(it))) }
    cache.save()
    if (problemsByPath.isNotEmpty()) throw AssetDatabaseRejected(problems)
  }

  public companion object {
    /**
     * Reads every asset under [root] and watches it with [watcher]. Throws [AssetDatabaseRejected],
     * naming each file, when a GUID is used twice, a meta file has no source or a source has no
     * meta file.
     */
    public fun load(
      root: Path,
      watcher: FileWatcher,
      guids: GuidSource = GuidSource.random,
    ): AssetDatabase {
      val database = AssetDatabase(root.toAbsolutePath().normalize(), watcher, guids)
      database.scan()
      watcher.watch(database.root)
      return database
    }
  }
}

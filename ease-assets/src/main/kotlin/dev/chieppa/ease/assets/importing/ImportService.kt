package dev.chieppa.ease.assets.importing

import dev.chieppa.ease.assets.Guid
import dev.chieppa.ease.assets.associations.FileAssociations
import dev.chieppa.ease.assets.associations.Registration
import dev.chieppa.ease.assets.database.AssetDatabase
import dev.chieppa.ease.assets.database.AssetProblem
import dev.chieppa.ease.assets.meta.MetaFiles
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

/**
 * Runs the importers over a project's assets. Each source gets the one importer the file
 * associations pick for it, or the one its sidecar or nearest folder preset names; its settings are
 * the folder presets' settings with the sidecar's on top. The cooked assets are kept in this
 * machine's cache under a hash of source, settings, importer version and every other file the
 * importer read through its [ImportContext], so [refresh] runs an importer only when one of those
 * changed. Each cooked asset is also known by its sub-asset GUID ([subAsset]).
 *
 * A source whose import fails, or whose importer is unknown or ambiguous, is listed in [problems]
 * and keeps its last good result. A source no importer matches has no result: when an importer's
 * registration is closed, the results it made go with it. Registering or closing an importer
 * refreshes at once, until [close].
 */
public class ImportService(
  private val database: AssetDatabase,
  associations: FileAssociations,
) : AutoCloseable {
  private val cache = ImportCache(database.root)
  private val attempts = ImportAttempts(database.root)
  private val sources = SourceImport(database, associations, cache, attempts)
  private val index = CookedIndex(cache, attempts)
  private val listener: Registration = associations.onChange {
    if (it === Importer.kind) refreshAfterChange()
  }

  /** What stops each source from importing right now, by source path. */
  public var problems: List<ImportProblem> = emptyList()
    private set

  /** The last good cooked assets of [guid], sorted by name; empty when it has none. */
  public fun cooked(guid: Guid): List<CookedAsset> = cache.read(guid)

  /** The names of [guid]'s last good cooked assets, sorted; empty when it has none. */
  public fun cookedNames(guid: Guid): List<String> = cache.names(guid)

  /** The file holding [guid]'s cooked asset [name], or null when its last good result has none. */
  public fun cookedFile(guid: Guid, name: String): Path? = cache.file(guid, name)

  /**
   * The cooked asset whose sub-asset GUID is [guid] (its source's GUID named by its name), or null
   * when no last good result holds one.
   */
  public fun subAsset(guid: Guid): SubAsset? = index.subAsset(guid)

  /**
   * The sources whose last good result read the file at [path] (from the content folder) besides
   * their own source, or whose latest failed import asked for it.
   */
  public fun dependents(path: Path): List<Guid> = index.dependents(path)

  /**
   * Imports the one source [guid] again when its inputs changed, as [refresh] would, and returns
   * whether a new result was stored. A failure is kept in [problems] in place of any earlier one
   * for that source.
   */
  public fun refresh(guid: Guid): Boolean {
    val record = database[guid] ?: return false
    val outcome = sources.importOne(record)
    problems =
      problems.filter { it.source != record.path } +
        listOfNotNull((outcome as? Outcome.Failed)?.let { ImportProblem(record.path, it.reason) })
    index.rebuild()
    return outcome is Outcome.Done && outcome.ran
  }

  /**
   * Imports every source that has no result for its current inputs, and removes the results of
   * assets that no importer matches or that are gone.
   */
  public fun refresh(): ImportReport {
    val imported = mutableListOf<Path>()
    val found = mutableListOf<ImportProblem>()
    val keep = HashSet<Guid>()
    var upToDate = 0
    for (record in database.assets.sortedBy { it.path.toString() }) {
      if (Files.isDirectory(database.root.resolve(record.path))) continue
      when (val outcome = sources.importOne(record)) {
        is Outcome.Skipped -> Unit
        is Outcome.Failed -> {
          keep += record.guid
          found += ImportProblem(record.path, outcome.reason)
        }
        is Outcome.Done -> {
          keep += record.guid
          if (outcome.ran) {
            imported.add(record.path)
          } else {
            upToDate++
          }
        }
      }
    }
    val unreadable = unreadableSources()
    for (guid in cache.guids().filter { it !in keep }) {
      val source = cache.sourceOf(guid)
      if (source != null && source in unreadable) {
        found += ImportProblem(source, "cannot be read now; its last good result is kept")
      } else {
        cache.remove(guid)
      }
    }
    problems = found
    index.rebuild()
    return ImportReport(imported, upToDate, found)
  }

  /** The sources whose meta file or contents the database could not read. */
  private fun unreadableSources(): Set<Path> =
    database.problems
      .mapNotNull {
        when (it) {
          is AssetProblem.UnreadableMeta -> MetaFiles.sourceOf(it.meta)
          is AssetProblem.UnreadableSource -> it.source
          else -> null
        }
      }
      .toSet()

  /** Refreshes for a registration change; a failure becomes a problem, never an exception. */
  @Suppress("TooGenericExceptionCaught") // The caller is a registration, which must not fail.
  private fun refreshAfterChange() {
    try {
      refresh()
    } catch (failure: IOException) {
      problems = listOf(ImportProblem(Path.of(""), "refresh failed: ${failure.message}"))
    } catch (failure: RuntimeException) {
      problems = listOf(ImportProblem(Path.of(""), "refresh failed: ${failure.message}"))
    }
  }

  override fun close() {
    listener.close()
  }
}

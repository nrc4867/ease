package dev.chieppa.ease.assets.importing

import dev.chieppa.ease.assets.Guid
import dev.chieppa.ease.assets.associations.FileAssociations
import dev.chieppa.ease.assets.associations.Registration
import dev.chieppa.ease.assets.database.AssetDatabase
import dev.chieppa.ease.assets.database.AssetRecord
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.serialization.json.JsonObject

/**
 * Runs the importers over a project's assets. Each source gets the one importer the file
 * associations pick for it, or the one its sidecar or nearest folder preset names; its settings are
 * the folder presets' settings with the sidecar's on top. The cooked assets are kept in this
 * machine's cache under a hash of source, settings and importer version, so [refresh] runs an
 * importer only when one of those changed.
 *
 * A source whose import fails, or whose importer is unknown or ambiguous, is listed in [problems]
 * and keeps its last good result. A source no importer matches has no result: when an importer's
 * registration is closed, the results it made go with it. Registering or closing an importer
 * refreshes at once, until [close].
 */
public class ImportService(
  private val database: AssetDatabase,
  private val associations: FileAssociations,
) : AutoCloseable {
  private val cache = ImportCache(database.root)
  private val listener: Registration = associations.onChange { if (it === Importer.kind) refresh() }

  /** What stops each source from importing right now, by source path. */
  public var problems: List<ImportProblem> = emptyList()
    private set

  /** The last good cooked assets of [guid], sorted by name; empty when it has none. */
  public fun cooked(guid: Guid): List<CookedAsset> = cache.read(guid)

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
      when (val outcome = importOne(record)) {
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
    cache.guids().filter { it !in keep }.forEach { cache.remove(it) }
    problems = found
    return ImportReport(imported, upToDate, found)
  }

  override fun close() {
    listener.close()
  }

  private fun importOne(record: AssetRecord): Outcome =
    try {
      importChosen(record, Choice.of(database, associations, record))
    } catch (failure: ChoiceFailure) {
      Outcome.Failed(failure.message.orEmpty())
    }

  private fun importChosen(record: AssetRecord, choice: Choice): Outcome {
    val importer = choice.importer ?: return Outcome.Skipped
    val key = ImportKey.of(importer, record.contentHash, choice.settings)
    return if (cache.currentKey(record.guid) == key) {
      Outcome.Done(ran = false)
    } else {
      run(record, importer, key, choice.settings)
    }
  }

  @Suppress("TooGenericExceptionCaught") // An importer is contributor code; any failure is data.
  private fun run(record: AssetRecord, importer: Importer, key: String, settings: JsonObject) =
    try {
      val cooked = importer.import(database.read(record), settings)
      cache.store(record.guid, key, cooked.sortedBy { it.name })
      Outcome.Done(ran = true)
    } catch (failure: IOException) {
      Outcome.Failed("cannot be imported: ${failure.message}")
    } catch (failure: RuntimeException) {
      Outcome.Failed("${importer.id} failed: ${failure.message}")
    }

  private sealed interface Outcome {
    data object Skipped : Outcome

    class Done(val ran: Boolean) : Outcome

    class Failed(val reason: String) : Outcome
  }
}

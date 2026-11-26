package dev.chieppa.ease.assets.importing

import dev.chieppa.ease.assets.ContentHash
import dev.chieppa.ease.assets.associations.FileAssociations
import dev.chieppa.ease.assets.database.AssetDatabase
import dev.chieppa.ease.assets.database.AssetRecord
import java.io.IOException
import kotlinx.serialization.json.JsonObject

/** How one source's import went. */
internal sealed interface Outcome {
  /** No importer matches the source. */
  data object Skipped : Outcome

  /** The source has a result for its current inputs; [ran] says whether it was just made. */
  class Done(val ran: Boolean) : Outcome

  /** The source could not be imported, for [reason]; its last good result stays. */
  class Failed(val reason: String) : Outcome
}

/**
 * Imports one source at a time into [cache]: chooses its importer and settings, skips it when the
 * key of its current inputs (its bytes, its settings and the files its last import read) is the
 * current one, and otherwise runs the importer with a [SourceContext] and stores the result under
 * the key of what it read. A failed import records the files it asked for, so one of them appearing
 * or changing imports the source again.
 */
internal class SourceImport(
  private val database: AssetDatabase,
  private val associations: FileAssociations,
  private val cache: ImportCache,
  private val attempts: ImportAttempts,
) {
  /** Imports [record] when its inputs changed. */
  fun importOne(record: AssetRecord): Outcome =
    try {
      val choice = Choice.of(database, associations, record)
      choice.importer?.let { readAndImport(record, it, choice.settings) } ?: Outcome.Skipped
    } catch (failure: ChoiceFailure) {
      Outcome.Failed(failure.message.orEmpty())
    }

  private fun readAndImport(record: AssetRecord, importer: Importer, settings: JsonObject) =
    try {
      importBytes(record, importer, settings, database.read(record))
    } catch (failure: IOException) {
      Outcome.Failed("cannot be read: ${failure.message}")
    }

  private fun importBytes(
    record: AssetRecord,
    importer: Importer,
    settings: JsonObject,
    bytes: ByteArray,
  ): Outcome {
    val hash = ContentHash.of(bytes)
    val inputs = SourceContext.hashesNow(database.root, cache.inputs(record.guid))
    val key = ImportKey.of(importer, hash, settings, inputs)
    return if (cache.currentKey(record.guid) == key) {
      Outcome.Done(ran = false)
    } else {
      run(record, importer, hash, settings, bytes)
    }
  }

  @Suppress("TooGenericExceptionCaught") // An importer is contributor code; any failure is data.
  private fun run(
    record: AssetRecord,
    importer: Importer,
    hash: String,
    settings: JsonObject,
    bytes: ByteArray,
  ): Outcome {
    val context = SourceContext(database.root, record.path, record.guid)
    val outcome =
      try {
        val cooked = context.use { importer.import(bytes, settings, it) }
        val key = ImportKey.of(importer, hash, settings, context.inputs)
        cache.store(record.guid, record.path, key, cooked.sortedBy { it.name }, context.inputs.keys)
        attempts.succeeded(record.guid)
        Outcome.Done(ran = true)
      } catch (failure: IOException) {
        Outcome.Failed("cannot be imported: ${failure.message}")
      } catch (failure: RuntimeException) {
        Outcome.Failed("${importer.id} failed: ${failure.message}")
      }
    if (outcome is Outcome.Failed) attempts.failed(record.guid, record.path, context.attempted)
    return outcome
  }
}

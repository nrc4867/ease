package dev.chieppa.ease.assets.loading

import dev.chieppa.ease.assets.database.AssetDatabase
import dev.chieppa.ease.assets.database.AssetRecord
import java.io.IOException

/** Marks every handle in [entries] as failed with [reason], then throws [AssetOutOfDate]. */
internal fun refuse(entries: List<StoreEntry<*>>, reason: String): Nothing {
  entries.forEach { it.handle.fail(reason) }
  throw AssetOutOfDate(reason)
}

/** [record]'s bytes, or when they cannot be read, every handle in [entries] refused for it. */
@Suppress("SwallowedException") // The reason is passed on in the refusal's message.
internal fun AssetDatabase.readOrRefuse(
  record: AssetRecord,
  entries: List<StoreEntry<*>>,
): ByteArray =
  try {
    read(record)
  } catch (failure: IOException) {
    refuse(entries, "${record.path} cannot be read: ${failure.message}")
  }

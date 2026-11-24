package dev.chieppa.ease.assets.loading

import dev.chieppa.ease.assets.ContentHash
import dev.chieppa.ease.assets.Guid
import dev.chieppa.ease.assets.associations.FileAssociations
import dev.chieppa.ease.assets.associations.ProjectFile
import dev.chieppa.ease.assets.database.AssetDatabase
import dev.chieppa.ease.assets.database.AssetRecord
import java.io.IOException
import java.nio.file.Files
import kotlin.reflect.KClass

/** The contents hash a reload carries when the asset's file is gone. */
public const val GONE_HASH: String = ""

/**
 * The assets a running game holds. [handle] loads an asset once through the loader the file
 * associations pick for its file, and every later call returns the same handle. [reload] re-reads
 * the file for every handle on that asset.
 */
public class AssetStore(
  /** The database the assets are looked up in. */
  public val database: AssetDatabase,
  private val associations: FileAssociations,
) {
  private val loaded = HashMap<Guid, MutableList<Entry<*>>>()

  /** Whether any handle on [guid] exists. */
  public fun isLoaded(guid: Guid): Boolean = guid in loaded

  /**
   * The handle for asset [guid] as a [type], loading it on first use. Throws [AssetLoadException]
   * when the asset or a loader for it is missing, or the file does not load.
   */
  public fun <T : Any> handle(guid: Guid, type: KClass<T>): AssetHandle<T> =
    existing(guid, type) ?: load(guid, type)

  /**
   * Re-reads the file of asset [guid] for every handle on it. [expectedHash] is the hash the change
   * was made for, or [GONE_HASH] when the file is gone; a file that differs from it throws
   * [AssetOutOfDate]. A loader that fails keeps the handle's last good value and sets its error.
   */
  public fun reload(guid: Guid, expectedHash: String) {
    val entries = loaded[guid] ?: return
    val record = database[guid]
    if (record == null || expectedHash == GONE_HASH) {
      entries.forEach { it.handle.fail("The asset's source file is gone") }
      return
    }
    val bytes = read(record)
    if (ContentHash.of(bytes) != expectedHash) {
      throw AssetOutOfDate("${record.path} on this machine is not the version the change names")
    }
    entries.forEach { it.reload(guid, bytes) }
  }

  /** The handle already loaded for [guid] as a [type]; walks by index so it allocates nothing. */
  @Suppress("UNCHECKED_CAST") // Entries are looked up by the type they were made for.
  private fun <T : Any> existing(guid: Guid, type: KClass<T>): AssetHandle<T>? {
    val entries = loaded[guid]
    var found: AssetHandle<T>? = null
    if (entries != null) {
      for (i in entries.indices) {
        if (entries[i].loader.type == type) found = entries[i].handle as AssetHandle<T>
      }
    }
    return found
  }

  private fun <T : Any> load(guid: Guid, type: KClass<T>): AssetHandle<T> {
    val record = database[guid] ?: throw AssetLoadException("No asset has GUID $guid")
    val loader = loaderFor(record, type)
    val entry = Entry(AssetHandle(guid, firstLoad(record, loader)), loader)
    loaded.getOrPut(guid) { mutableListOf() }.add(entry)
    return entry.handle
  }

  @Suppress("TooGenericExceptionCaught") // A loader is contributor code; any failure is data.
  private fun <T : Any> firstLoad(record: AssetRecord, loader: AssetLoader<T>): T =
    try {
      loader.load(record.guid, read(record))
    } catch (failure: IOException) {
      throw AssetLoadException("${record.path} cannot be read: ${failure.message}", failure)
    } catch (failure: RuntimeException) {
      throw AssetLoadException("${record.path} does not load: ${failure.message}", failure)
    }

  private fun read(record: AssetRecord): ByteArray =
    Files.readAllBytes(database.root.resolve(record.path))

  private fun <T : Any> loaderFor(record: AssetRecord, type: KClass<T>): AssetLoader<T> {
    val file = ProjectFile.under(database.root, record.path)
    val loader =
      associations.resolve(AssetLoader.kind, file).firstOrNull { it.type == type }
        ?: throw AssetLoadException("No loader for ${record.path} produces ${type.simpleName}")
    @Suppress("UNCHECKED_CAST") // Its type was just compared with the requested one.
    return loader as AssetLoader<T>
  }

  private class Entry<T : Any>(val handle: AssetHandle<T>, val loader: AssetLoader<T>) {
    @Suppress("TooGenericExceptionCaught") // A loader is contributor code; any failure is data.
    fun reload(guid: Guid, bytes: ByteArray) {
      try {
        handle.replace(loader.load(guid, bytes))
      } catch (failure: RuntimeException) {
        handle.fail("Does not load: ${failure.message}")
      }
    }
  }
}

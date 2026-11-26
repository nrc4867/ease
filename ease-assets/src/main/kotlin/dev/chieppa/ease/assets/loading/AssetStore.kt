package dev.chieppa.ease.assets.loading

import dev.chieppa.ease.assets.ContentHash
import dev.chieppa.ease.assets.Guid
import dev.chieppa.ease.assets.associations.FileAssociations
import dev.chieppa.ease.assets.database.AssetDatabase
import dev.chieppa.ease.assets.database.AssetRecord
import dev.chieppa.ease.assets.importing.ImportService
import kotlin.reflect.KClass

/** The contents hash a reload carries when the asset's file is gone. */
public const val GONE_HASH: String = ""

/**
 * The assets a running game holds. [handle] loads an asset once and every later call returns the
 * same handle. A source asset loads through the loader the file associations pick for its file; one
 * with no such loader for the type asked, such as a model, loads from the one cooked asset of its
 * last import that a loader turns into that type. A sub-asset GUID (a source's GUID named by a
 * cooked asset's name) loads that cooked asset, through the loader picked for its name. [reload]
 * re-reads an asset for every handle on it, and with [imports] imports it, and the sources whose
 * import read its file, again, reloading their cooked handles.
 *
 * A reload's replaced values that are [AutoCloseable], such as meshes holding native and GPU
 * memory, are closed at the second [frameBoundary] after it, when no frame draws them any more.
 */
public class AssetStore(
  /** The database the assets are looked up in. */
  public val database: AssetDatabase,
  associations: FileAssociations,
  /** Where cooked assets come from; without it only sources load. */
  public val imports: ImportService? = null,
) {
  private val loaded = HashMap<Guid, MutableList<StoreEntry<*>>>()
  private val factory = EntryFactory(database, imports, LoaderChoice(database.root, associations))

  private val retired = RetiredValues()

  /** Counts [reload] calls, so code that remembers a failed load knows when to try again. */
  public var changes: Int = 0
    private set

  /** Whether any handle on [guid] exists. */
  public fun isLoaded(guid: Guid): Boolean = guid in loaded

  /**
   * Whether a change to [guid]'s file matters to this store: a handle on it exists, or with
   * [imports] any change does, since it may be a source to import or a file an import reads.
   */
  public fun follows(guid: Guid): Boolean = guid in loaded || imports != null

  /**
   * Marks the end of a frame: the values reloads replaced before the previous boundary are closed.
   * Call it once per frame, after every world sharing the store has extracted and before drawing.
   */
  public fun frameBoundary() {
    retired.boundary()
  }

  /** How many replaced values wait for a [frameBoundary] to be closed. */
  public val retiredCount: Int
    get() = retired.size

  /** Whether [guid] names a source asset or a cooked sub-asset this store can load. */
  public fun has(guid: Guid): Boolean = database[guid] != null || imports?.subAsset(guid) != null

  /**
   * The handle for asset [guid] as a [type], loading it on first use. Throws [AssetLoadException]
   * when the asset or a loader for it is missing, or the file does not load.
   */
  public fun <T : Any> handle(guid: Guid, type: KClass<T>): AssetHandle<T> =
    existing(guid, type) ?: load(guid, type)

  /**
   * Re-reads asset [guid] for every handle on it. [expectedHash] is the hash the change was made
   * for, or [GONE_HASH] when the file is gone; a file that differs from it throws [AssetOutOfDate],
   * and so does a file that cannot be read; either way the handles' errors say why. Handles on the
   * cooked assets of [guid], or of sources whose import read its file, are imported again and
   * reloaded. A loader that fails keeps the handle's last good value and sets its error.
   */
  public fun reload(guid: Guid, expectedHash: String) {
    changes++
    val record = database[guid]
    val direct = loaded[guid].orEmpty().filter { it.origin is Origin.Source }
    val cooked = cookedOf(guid)
    if (direct.isNotEmpty() || cooked.isNotEmpty()) {
      val all = direct + cooked
      if (record == null || expectedHash == GONE_HASH) {
        all.forEach { it.handle.fail("The asset's source file is gone") }
        imports?.refresh()
        return
      }
      val bytes = database.readOrRefuse(record, all)
      if (ContentHash.of(bytes) != expectedHash) {
        refuse(all, "${record.path} on this machine is not the version the change names")
      }
      direct.forEach { it.reloadSource(bytes, retired) }
    }
    if (record != null) {
      reimport(guid, cooked)
      reloadDependents(record)
    } else {
      imports?.refresh()
    }
  }

  private fun reloadDependents(record: AssetRecord) {
    val imports = imports ?: return
    for (parent in imports.dependents(record.path)) {
      if (parent != record.guid) reimport(parent, cookedOf(parent))
    }
  }

  private fun reimport(parent: Guid, cooked: List<StoreEntry<*>>) {
    val imports = imports ?: return
    imports.refresh(parent)
    cooked.forEach { it.reloadCooked(imports, retired) }
  }

  /** Every handle loaded from a cooked asset of [parent]. */
  private fun cookedOf(parent: Guid): List<StoreEntry<*>> =
    loaded.values.flatten().filter { (it.origin as? Origin.Cooked)?.parent == parent }

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
    val entry = factory.create(guid, type)
    loaded.getOrPut(guid) { mutableListOf() }.add(entry)
    return entry.handle
  }
}

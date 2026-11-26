package dev.chieppa.ease.assets.loading

import dev.chieppa.ease.assets.Guid
import dev.chieppa.ease.assets.database.AssetDatabase
import dev.chieppa.ease.assets.database.AssetRecord
import dev.chieppa.ease.assets.importing.ImportService
import kotlin.reflect.KClass

/**
 * Makes an [AssetStore]'s first load of an asset: a source through its own loader, else through the
 * one cooked asset of its import that loads as the type, and a sub-asset GUID through the loader
 * for its cooked file's name.
 */
internal class EntryFactory(
  private val database: AssetDatabase,
  private val imports: ImportService?,
  private val loaders: LoaderChoice,
) {
  /** The first load of [guid] as a [type]; throws [AssetLoadException] when it cannot load. */
  fun <T : Any> create(guid: Guid, type: KClass<T>): StoreEntry<T> =
    database[guid]?.let { fromSource(it, type) } ?: fromSubAsset(guid, type)

  private fun <T : Any> fromSource(record: AssetRecord, type: KClass<T>): StoreEntry<T> {
    val guid = record.guid
    val own = loaders.forSource(record, type)
    val cooked =
      if (own == null) imports?.let { loaders.cookedOfSource(it, record, type) } else null
    return when {
      own != null -> StoreEntry.first(guid, own, Origin.Source(record), database)
      cooked != null ->
        StoreEntry.first(
          guid,
          cooked.second,
          Origin.Cooked(guid, cooked.first, record.path),
          checkNotNull(imports),
        )
      else -> throw AssetLoadException("No loader for ${record.path} produces ${type.simpleName}")
    }
  }

  private fun <T : Any> fromSubAsset(guid: Guid, type: KClass<T>): StoreEntry<T> {
    val imports = imports
    val sub = imports?.subAsset(guid) ?: throw AssetLoadException("No asset has GUID $guid")
    val parent = database[sub.parent]?.path?.toString() ?: sub.parent.toString()
    val loader =
      loaders.forCooked(imports, sub.parent, sub.name, type)
        ?: throw AssetLoadException(
          "No loader for $parent's ${sub.name} produces ${type.simpleName}"
        )
    return StoreEntry.first(guid, loader, Origin.Cooked(sub.parent, sub.name, parent), imports)
  }
}

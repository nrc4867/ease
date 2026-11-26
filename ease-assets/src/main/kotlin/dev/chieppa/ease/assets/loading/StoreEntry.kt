package dev.chieppa.ease.assets.loading

import dev.chieppa.ease.assets.Guid
import dev.chieppa.ease.assets.database.AssetDatabase
import dev.chieppa.ease.assets.database.AssetRecord
import dev.chieppa.ease.assets.importing.ImportService
import java.io.IOException
import java.nio.file.Path

/** Where a handle's value is read from. */
internal sealed interface Origin {
  /** The asset's own source file. */
  class Source(val record: AssetRecord) : Origin

  /** The cooked asset [name] of the source [parent], whose path or GUID is [parentName]. */
  class Cooked(val parent: Guid, val name: String, val parentName: Any) : Origin
}

/** One handle of an [AssetStore], with the loader and origin it reloads from. */
internal class StoreEntry<T : Any>(
  val guid: Guid,
  val handle: AssetHandle<T>,
  val loader: AssetLoader<T>,
  val origin: Origin,
) {
  /** Loads the value again from the source's [bytes], handing the replaced one to [retired]. */
  @Suppress("TooGenericExceptionCaught") // A loader is contributor code; any failure is data.
  fun reloadSource(bytes: ByteArray, retired: RetiredValues) {
    try {
      retired.add(handle.replace(loader.load(guid, bytes)))
    } catch (failure: RuntimeException) {
      handle.fail("Does not load: ${failure.message}")
    }
  }

  /** Loads the value again from its cooked file in [imports], retiring the replaced one. */
  @Suppress("TooGenericExceptionCaught") // A loader is contributor code; any failure is data.
  fun reloadCooked(imports: ImportService, retired: RetiredValues) {
    val cooked = origin as Origin.Cooked
    val file = imports.cookedFile(cooked.parent, cooked.name)
    if (file == null) {
      handle.fail("${cooked.parentName} no longer imports ${cooked.name}")
      return
    }
    try {
      retired.add(handle.replace(loader.load(guid, file)))
    } catch (failure: IOException) {
      handle.fail("${cooked.name} cannot be read: ${failure.message}")
    } catch (failure: RuntimeException) {
      handle.fail("Does not load: ${failure.message}")
    }
  }

  companion object {
    /** The first load of [guid] from its source, through [loader]. */
    fun <T : Any> first(
      guid: Guid,
      loader: AssetLoader<T>,
      origin: Origin.Source,
      database: AssetDatabase,
    ): StoreEntry<T> =
      StoreEntry(
        guid,
        AssetHandle(
          guid,
          loading(origin.record.path) {
            loader.load(guid, database.read(origin.record))
          },
        ),
        loader,
        origin,
      )

    /** The first load of [guid] from its cooked file in [imports], through [loader]. */
    fun <T : Any> first(
      guid: Guid,
      loader: AssetLoader<T>,
      origin: Origin.Cooked,
      imports: ImportService,
    ): StoreEntry<T> {
      val what = "${origin.parentName}'s ${origin.name}"
      val value =
        loading(what) {
          val file: Path =
            imports.cookedFile(origin.parent, origin.name)
              ?: throw IOException("its last import holds no ${origin.name}")
          loader.load(guid, file)
        }
      return StoreEntry(guid, AssetHandle(guid, value), loader, origin)
    }

    @Suppress("TooGenericExceptionCaught") // A loader is contributor code; any failure is data.
    private inline fun <T> loading(what: Any, load: () -> T): T =
      try {
        load()
      } catch (failure: IOException) {
        throw AssetLoadException("$what cannot be read: ${failure.message}", failure)
      } catch (failure: AssetLoadException) {
        throw failure
      } catch (failure: RuntimeException) {
        throw AssetLoadException("$what does not load: ${failure.message}", failure)
      }
  }
}

package dev.chieppa.ease.assets.loading

import dev.chieppa.ease.assets.Guid
import dev.chieppa.ease.assets.associations.AmbiguousContributors
import dev.chieppa.ease.assets.associations.FileAssociations
import dev.chieppa.ease.assets.associations.ProjectFile
import dev.chieppa.ease.assets.database.AssetRecord
import dev.chieppa.ease.assets.importing.ImportService
import java.nio.file.Files
import java.nio.file.Path
import kotlin.reflect.KClass

private const val SNIFF_BYTES = 512

/** Picks the loader of a type for a source file or a cooked file, by the file associations. */
internal class LoaderChoice(private val root: Path, private val associations: FileAssociations) {
  /** The loader of [type] for [record]'s own file, or null when none matches it. */
  fun <T : Any> forSource(record: AssetRecord, type: KClass<T>): AssetLoader<T>? =
    pick(ProjectFile.under(root, record.path), type)

  /** The loader of [type] for [parent]'s cooked asset [name], or null when none matches it. */
  fun <T : Any> forCooked(
    imports: ImportService,
    parent: Guid,
    name: String,
    type: KClass<T>,
  ): AssetLoader<T>? {
    val file =
      ProjectFile(Path.of(name)) {
        imports.cookedFile(parent, name)?.let { path ->
          Files.newInputStream(path).use { it.readNBytes(SNIFF_BYTES) }
        } ?: ByteArray(0)
      }
    return pick(file, type)
  }

  /**
   * The one cooked asset of [record] that a loader turns into [type], with that loader; null when
   * none does. Two or more is an [AssetLoadException] naming them, since the source's GUID then
   * does not say which is meant.
   */
  fun <T : Any> cookedOfSource(
    imports: ImportService,
    record: AssetRecord,
    type: KClass<T>,
  ): Pair<String, AssetLoader<T>>? {
    val found =
      imports.cookedNames(record.guid).mapNotNull { name ->
        forCooked(imports, record.guid, name, type)?.let { name to it }
      }
    if (found.size > 1) {
      throw AssetLoadException(
        "${record.path} imports ${found.size} assets that load as ${type.simpleName} " +
          "(${found.joinToString { it.first }}); name one by its sub-asset GUID"
      )
    }
    return found.singleOrNull()
  }

  private fun <T : Any> pick(file: ProjectFile, type: KClass<T>): AssetLoader<T>? {
    val loader =
      try {
        associations.resolve(AssetLoader.kind, file) { it.type == type }.firstOrNull()
      } catch (ambiguous: AmbiguousContributors) {
        throw AssetLoadException(ambiguous.message.orEmpty(), ambiguous)
      }
    @Suppress("UNCHECKED_CAST") // Its type was just compared with the requested one.
    return loader as AssetLoader<T>?
  }
}

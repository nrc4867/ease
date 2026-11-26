package dev.chieppa.ease.assets.importing

import dev.chieppa.ease.assets.Guid
import dev.chieppa.ease.assets.database.CACHE_FOLDER
import dev.chieppa.ease.assets.database.CacheFolder
import dev.chieppa.ease.platform.files.FileTrees
import java.nio.file.Files
import java.nio.file.Path

private const val IMPORTS_FOLDER = "imports"
private const val CURRENT_FILE = "current"
/** The file in an asset's folder naming the source path its results came from. */
internal const val SOURCE_FILE: String = "source"
private const val INPUTS_FILE = "inputs"

/** The folder of everything stored for [guid] under the content folder [root]. */
internal fun importFolder(root: Path, guid: Guid): Path =
  root.resolve(CACHE_FOLDER).resolve(IMPORTS_FOLDER).resolve(guid.toString())

/**
 * Cooked assets on this machine, under `.ease/cache/imports/<guid>/<key>/`. Each asset's `current`
 * file names the key of its last good result, so a result is replaced only once a new one is
 * complete and a failed import leaves the old one in place; its `source` file names the source path
 * the result came from.
 */
internal class ImportCache(private val root: Path) {
  /** The key of the last good result of [guid], or null when it has none. */
  fun currentKey(guid: Guid): String? {
    val file = importFolder(root, guid).resolve(CURRENT_FILE)
    return if (Files.isRegularFile(file)) Files.readString(file) else null
  }

  /** The source path [guid]'s result was made from, or null when it has none. */
  fun sourceOf(guid: Guid): Path? {
    val file = importFolder(root, guid).resolve(SOURCE_FILE)
    return if (Files.isRegularFile(file)) Path.of(Files.readString(file)) else null
  }

  /**
   * Stores [cooked] for [guid], made from [source], under [key] and makes it current, dropping
   * older results. Throws [IllegalArgumentException] for two assets whose names are equal or differ
   * only in case, which a case-insensitive disk would merge.
   */
  fun store(
    guid: Guid,
    source: Path,
    key: String,
    cooked: List<CookedAsset>,
    inputs: Collection<String> = emptyList(),
  ) {
    require(cooked.map { it.name.lowercase() }.toSet().size == cooked.size) {
      "The importer produced two cooked assets with the same name: ${cooked.map { it.name }}"
    }
    CacheFolder.ensure(root)
    val asset = importFolder(root, guid)
    val target = asset.resolve(key)
    Files.createDirectories(target)
    cooked.forEach { Files.write(target.resolve(it.name), it.bytes) }
    Files.writeString(asset.resolve(SOURCE_FILE), source.joinToString("/"))
    Files.write(asset.resolve(INPUTS_FILE), inputs)
    Files.writeString(asset.resolve(CURRENT_FILE), key)
    Files.list(asset).use { entries ->
      entries
        .filter { Files.isDirectory(it) && it.fileName.toString() != key }
        .forEach { FileTrees.deleteRecursively(it) }
    }
  }

  /** The last good cooked assets of [guid], sorted by name; empty when it has none. */
  fun read(guid: Guid): List<CookedAsset> {
    val key = currentKey(guid) ?: return emptyList()
    val target = importFolder(root, guid).resolve(key)
    return Files.list(target).use { entries ->
      entries
        .sorted(compareBy { it.fileName.toString() })
        .map { CookedAsset(it.fileName.toString(), Files.readAllBytes(it)) }
        .toList()
    }
  }

  /** The paths of the other files [guid]'s last good result read, from the content folder. */
  fun inputs(guid: Guid): List<String> {
    val file = importFolder(root, guid).resolve(INPUTS_FILE)
    return if (Files.isRegularFile(file)) Files.readAllLines(file).filter { it.isNotEmpty() }
    else emptyList()
  }

  /** The names of [guid]'s last good cooked assets, sorted; empty when it has none. */
  fun names(guid: Guid): List<String> {
    val folder = currentFolder(guid) ?: return emptyList()
    return Files.list(folder).use { entries ->
      entries.map { it.fileName.toString() }.sorted().toList()
    }
  }

  /** The file holding [guid]'s cooked asset [name], or null when its last good result has none. */
  fun file(guid: Guid, name: String): Path? =
    currentFolder(guid)?.resolve(name)?.takeIf { Files.isRegularFile(it) }

  private fun currentFolder(guid: Guid): Path? =
    currentKey(guid)?.let { importFolder(root, guid).resolve(it) }?.takeIf { Files.isDirectory(it) }

  /** Every asset that has a stored result; files and folders that are not GUIDs are ignored. */
  fun guids(): List<Guid> {
    val folder = root.resolve(CACHE_FOLDER).resolve(IMPORTS_FOLDER)
    if (!Files.isDirectory(folder)) return emptyList()
    return Files.list(folder).use { entries ->
      entries
        .filter { Files.isDirectory(it) }
        .map { runCatching { Guid.parse(it.fileName.toString()) }.getOrNull() }
        .toList()
        .filterNotNull()
    }
  }

  /** Removes everything stored for [guid]. */
  fun remove(guid: Guid) {
    FileTrees.deleteRecursively(importFolder(root, guid))
  }
}

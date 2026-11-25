package dev.chieppa.ease.assets.importing

import dev.chieppa.ease.assets.Guid
import dev.chieppa.ease.assets.database.CACHE_FOLDER
import dev.chieppa.ease.assets.database.CacheFolder
import dev.chieppa.ease.platform.files.FileTrees
import java.nio.file.Files
import java.nio.file.Path

private const val IMPORTS_FOLDER = "imports"
private const val CURRENT_FILE = "current"
private const val SOURCE_FILE = "source"

/**
 * Cooked assets on this machine, under `.ease/cache/imports/<guid>/<key>/`. Each asset's `current`
 * file names the key of its last good result, so a result is replaced only once a new one is
 * complete and a failed import leaves the old one in place; its `source` file names the source path
 * the result came from.
 */
internal class ImportCache(private val root: Path) {
  private fun assetFolder(guid: Guid): Path =
    root.resolve(CACHE_FOLDER).resolve(IMPORTS_FOLDER).resolve(guid.toString())

  /** The key of the last good result of [guid], or null when it has none. */
  fun currentKey(guid: Guid): String? {
    val file = assetFolder(guid).resolve(CURRENT_FILE)
    return if (Files.isRegularFile(file)) Files.readString(file) else null
  }

  /** The source path [guid]'s result was made from, or null when it has none. */
  fun sourceOf(guid: Guid): Path? {
    val file = assetFolder(guid).resolve(SOURCE_FILE)
    return if (Files.isRegularFile(file)) Path.of(Files.readString(file)) else null
  }

  /**
   * Stores [cooked] for [guid], made from [source], under [key] and makes it current, dropping
   * older results. Throws [IllegalArgumentException] for two assets whose names are equal or differ
   * only in case, which a case-insensitive disk would merge.
   */
  fun store(guid: Guid, source: Path, key: String, cooked: List<CookedAsset>) {
    require(cooked.map { it.name.lowercase() }.toSet().size == cooked.size) {
      "The importer produced two cooked assets with the same name: ${cooked.map { it.name }}"
    }
    CacheFolder.ensure(root)
    val asset = assetFolder(guid)
    val target = asset.resolve(key)
    Files.createDirectories(target)
    cooked.forEach { Files.write(target.resolve(it.name), it.bytes) }
    Files.writeString(asset.resolve(SOURCE_FILE), source.joinToString("/"))
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
    val target = assetFolder(guid).resolve(key)
    return Files.list(target).use { entries ->
      entries
        .sorted(compareBy { it.fileName.toString() })
        .map { CookedAsset(it.fileName.toString(), Files.readAllBytes(it)) }
        .toList()
    }
  }

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
    FileTrees.deleteRecursively(assetFolder(guid))
  }
}

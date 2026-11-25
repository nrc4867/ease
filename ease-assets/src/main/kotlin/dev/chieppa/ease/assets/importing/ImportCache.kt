package dev.chieppa.ease.assets.importing

import dev.chieppa.ease.assets.Guid
import dev.chieppa.ease.assets.database.CacheFolder
import java.nio.file.Files
import java.nio.file.Path

private const val IMPORTS_FOLDER = "imports"
private const val CURRENT_FILE = "current"

/**
 * Cooked assets on this machine, under `.ease/cache/imports/<guid>/<key>/`. Each asset's `current`
 * file names the key of its last good result, so a result is replaced only once a new one is
 * complete and a failed import leaves the old one in place.
 */
internal class ImportCache(private val root: Path) {
  private val folder: Path
    get() = root.resolve(".ease/cache").resolve(IMPORTS_FOLDER)

  /** The key of the last good result of [guid], or null when it has none. */
  fun currentKey(guid: Guid): String? {
    val file = folder.resolve(guid.toString()).resolve(CURRENT_FILE)
    return if (Files.isRegularFile(file)) Files.readString(file) else null
  }

  /** Stores [cooked] for [guid] under [key] and makes it current, dropping older results. */
  fun store(guid: Guid, key: String, cooked: List<CookedAsset>) {
    CacheFolder.ensure(root)
    val asset = folder.resolve(guid.toString())
    val target = asset.resolve(key)
    Files.createDirectories(target)
    cooked.forEach { Files.write(target.resolve(it.name), it.bytes) }
    Files.writeString(asset.resolve(CURRENT_FILE), key)
    Files.list(asset).use { entries ->
      entries
        .filter { Files.isDirectory(it) && it.fileName.toString() != key }
        .forEach { deleteTree(it) }
    }
  }

  /** The last good cooked assets of [guid], sorted by name; empty when it has none. */
  fun read(guid: Guid): List<CookedAsset> {
    val key = currentKey(guid) ?: return emptyList()
    val target = folder.resolve(guid.toString()).resolve(key)
    return Files.list(target).use { entries ->
      entries.sorted().map { CookedAsset(it.fileName.toString(), Files.readAllBytes(it)) }.toList()
    }
  }

  /** Every asset that has a stored result. */
  fun guids(): List<Guid> {
    if (!Files.isDirectory(folder)) return emptyList()
    return Files.list(folder).use { entries ->
      entries.map { Guid.parse(it.fileName.toString()) }.toList()
    }
  }

  /** Removes everything stored for [guid]. */
  fun remove(guid: Guid) {
    deleteTree(folder.resolve(guid.toString()))
  }

  private fun deleteTree(path: Path) {
    if (!Files.exists(path)) return
    Files.walk(path).use { walk -> walk.sorted(Comparator.reverseOrder()).forEach(Files::delete) }
  }
}

package dev.chieppa.ease.assets.database

import dev.chieppa.ease.serialization.yaml.CanonicalYaml
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** The folder, inside the project, where this machine keeps what it learned about the files. */
internal const val CACHE_FOLDER = ".ease/cache"

private const val HASHES_FILE = "asset-hashes.yaml"
private const val HASHES_KEY = "hashes"
private const val SEPARATOR = "/"

/**
 * The last known content hash of each source path, kept per machine in `.ease/cache/` under the
 * project folder and never committed: the folder holds its own `.gitignore` that ignores everything
 * in it, so no repository setup is needed. A meta file whose source disappeared uses the hash
 * recorded for its path to find the file it was moved or renamed to. A missing or unreadable cache,
 * as in a fresh clone, is empty, and nothing is matched.
 */
internal class HashCache(private val root: Path) {
  private val folder = root.resolve(CACHE_FOLDER)
  private val file = folder.resolve(HASHES_FILE)
  private val hashes = HashMap<String, String>()
  private var dirty = false

  init {
    load()
  }

  /** The hash last recorded for the source at [path], or null when none is. */
  operator fun get(path: Path): String? = hashes[key(path)]

  /** Records [hash] for [path]. */
  fun put(path: Path, hash: String) {
    if (hashes.put(key(path), hash) != hash) dirty = true
  }

  /** Forgets [path]. */
  fun remove(path: Path) {
    if (hashes.remove(key(path)) != null) dirty = true
  }

  /** Forgets every path for which [keep] is false. */
  fun retain(keep: (Path) -> Boolean) {
    val before = hashes.size
    hashes.keys.removeIf { !keep(Path.of(it)) }
    if (hashes.size != before) dirty = true
  }

  /** Writes the cache when it changed. A cache that cannot be written is only a missed match. */
  @Suppress("SwallowedException")
  fun save() {
    if (!dirty) return
    try {
      CacheFolder.ensure(root)
      val tree =
        JsonObject(mapOf(HASHES_KEY to JsonObject(hashes.mapValues { JsonPrimitive(it.value) })))
      Files.writeString(file, CanonicalYaml.write(tree))
      dirty = false
    } catch (_: IOException) {
      return
    }
  }

  /** Whatever is wrong with the file, an unreadable cache is an empty one. */
  @Suppress("SwallowedException", "TooGenericExceptionCaught")
  private fun load() {
    try {
      val stored = CanonicalYaml.read(Files.readString(file)).jsonObject[HASHES_KEY]?.jsonObject
      stored?.forEach { (path, hash) ->
        hash.jsonPrimitive.contentOrNull?.let { hashes[path] = it }
      }
    } catch (_: Exception) {
      hashes.clear()
    }
  }

  private fun key(path: Path): String = path.joinToString(SEPARATOR)
}

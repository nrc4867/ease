package dev.chieppa.ease.assets.importing

import dev.chieppa.ease.assets.ContentHash
import dev.chieppa.ease.serialization.yaml.CanonicalYaml
import kotlinx.serialization.json.JsonObject

/**
 * The hash a cooked result is stored under: of the importer, the source, the settings, and every
 * other file the importer read, by path and contents hash.
 */
internal object ImportKey {
  /**
   * The key for [importer] run on a source with hash [sourceHash] under [settings], having read
   * [inputs] (contents hashes by path from the content folder).
   */
  fun of(
    importer: Importer,
    sourceHash: String,
    settings: JsonObject,
    inputs: Map<String, String> = emptyMap(),
  ): String {
    val lines =
      listOf(importer.id, importer.version.toString(), sourceHash, CanonicalYaml.write(settings)) +
        inputs.toSortedMap().map { (path, hash) -> "$path=$hash" }
    val text = lines.joinToString(LINE)
    return ContentHash.of(text.toByteArray(Charsets.UTF_8)).removePrefix("sha256:")
  }

  private const val LINE = "\n"
}

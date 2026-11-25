package dev.chieppa.ease.assets.importing

import dev.chieppa.ease.assets.ContentHash
import dev.chieppa.ease.serialization.yaml.CanonicalYaml
import kotlinx.serialization.json.JsonObject

/** The hash a cooked result is stored under: of the source, the settings and the importer. */
internal object ImportKey {
  /** The key for [importer] run on a source with hash [sourceHash] under [settings]. */
  fun of(importer: Importer, sourceHash: String, settings: JsonObject): String {
    val text = "${importer.id}\n${importer.version}\n$sourceHash\n${CanonicalYaml.write(settings)}"
    return ContentHash.of(text.toByteArray(Charsets.UTF_8)).removePrefix("sha256:")
  }
}

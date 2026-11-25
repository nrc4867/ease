package dev.chieppa.ease.cli.template

import dev.chieppa.ease.cli.CliFailure
import dev.chieppa.ease.serialization.format.FormatMigrations
import dev.chieppa.ease.serialization.format.UnsupportedFormatVersion
import dev.chieppa.ease.serialization.yaml.CanonicalYaml
import dev.chieppa.ease.serialization.yaml.YamlFormatException
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/** Reads a versioned YAML file, turning every way it can be wrong into a [CliFailure]. */
internal object VersionedYaml {
  /**
   * The [serializer]'s value in [text], migrated to [migrations]' current version. [label] names
   * the file in the failure: a file that is not YAML, not a mapping, from a newer Ease or not the
   * right shape.
   */
  fun <T> decode(
    serializer: KSerializer<T>,
    text: String,
    migrations: FormatMigrations,
    label: String,
  ): T =
    try {
      val tree = CanonicalYaml.read(text) as? JsonObject ?: fail(label, "is not a mapping")
      Json.decodeFromJsonElement(serializer, migrations.migrate(tree))
    } catch (failure: YamlFormatException) {
      fail(label, "is not valid YAML: ${failure.message}", failure)
    } catch (failure: UnsupportedFormatVersion) {
      fail(label, "cannot be read: ${failure.message}", failure)
    } catch (failure: SerializationException) {
      fail(label, "has the wrong shape: ${failure.message}", failure)
    }

  private fun fail(label: String, reason: String, cause: Throwable? = null): Nothing =
    throw CliFailure("$label $reason", cause)
}

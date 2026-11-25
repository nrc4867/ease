package dev.chieppa.ease.assets.meta

import dev.chieppa.ease.serialization.format.FormatMigrations
import dev.chieppa.ease.serialization.format.UnsupportedFormatVersion
import dev.chieppa.ease.serialization.yaml.CanonicalYaml
import dev.chieppa.ease.serialization.yaml.YamlFormatException
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonObject

/**
 * Reads and writes the small versioned YAML files next to assets: meta files and folder presets.
 */
internal object VersionedYamlFile {
  /**
   * The file at [file] migrated by [migrations] and decoded, without touching the file. Failures
   * come back as the message of the exception [fail] makes.
   */
  fun <T> read(
    file: Path,
    serializer: KSerializer<T>,
    migrations: FormatMigrations,
    fail: (String, Throwable) -> RuntimeException,
  ): T {
    val failure: Exception =
      try {
        val tree = CanonicalYaml.read(Files.readString(file))
        val mapping = tree as? JsonObject ?: throw YamlFormatException("not a mapping")
        val migrated = migrations.migrate(mapping)
        return CanonicalYaml.decode(serializer, CanonicalYaml.write(migrated))
      } catch (failure: YamlFormatException) {
        failure
      } catch (failure: UnsupportedFormatVersion) {
        failure
      } catch (failure: SerializationException) {
        failure
      } catch (failure: IllegalArgumentException) {
        failure
      } catch (failure: IOException) {
        failure
      }
    throw fail(failure.message.orEmpty(), failure)
  }

  /** [value] as canonical YAML, leaving out every field that is null. */
  fun <T> encode(serializer: KSerializer<T>, value: T): String =
    CanonicalYaml.encodeWithoutNulls(serializer, value)
}

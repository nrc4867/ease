package dev.chieppa.ease.serialization.format

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

/** The key every versioned file carries. */
public const val FORMAT_VERSION_KEY: String = "formatVersion"

/** Thrown when a file's format version cannot be brought to the current one. */
public class UnsupportedFormatVersion(message: String) : RuntimeException(message)

/**
 * The code that upgrades one file format: [steps] maps a version to the function that turns a file
 * of that version into one of the next. A file at [current] is already up to date. Migrating
 * returns a new tree and never touches the file on disk; writing the result is an explicit project
 * upgrade.
 */
public class FormatMigrations(
  public val current: Int,
  private val steps: Map<Int, (JsonObject) -> JsonObject> = emptyMap(),
) {
  init {
    require(steps.keys.all { it in 1 until current }) {
      "Migrations exist only for versions below $current: ${steps.keys}"
    }
  }

  /** The version [file] declares. Throws [UnsupportedFormatVersion] when it declares none. */
  public fun versionOf(file: JsonObject): Int =
    (file[FORMAT_VERSION_KEY] as? JsonPrimitive)?.intOrNull
      ?: throw UnsupportedFormatVersion("The file has no $FORMAT_VERSION_KEY")

  /**
   * [file] migrated to [current], with its version key updated. Throws [UnsupportedFormatVersion]
   * for a file from a newer Ease, or one whose version has no migration path.
   */
  public fun migrate(file: JsonObject): JsonObject {
    var version = versionOf(file)
    if (version > current) {
      throw UnsupportedFormatVersion(
        "The file is format version $version; this Ease reads up to $current"
      )
    }
    var result = file
    while (version < current) {
      val step =
        steps[version]
          ?: throw UnsupportedFormatVersion("No migration from format version $version")
      result = step(result)
      version++
    }
    return JsonObject(result + (FORMAT_VERSION_KEY to JsonPrimitive(current)))
  }
}

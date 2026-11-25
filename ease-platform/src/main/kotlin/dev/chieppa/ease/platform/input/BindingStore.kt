package dev.chieppa.ease.platform.input

import dev.chieppa.ease.serialization.yaml.CanonicalYaml
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private const val VERSION = 1

/**
 * One player's bindings as a canonical YAML file at [file]: for each context by name, its list of
 * action, control and scale. The file holds what the player's rebinding left, and loading puts it
 * over the game's defaults.
 */
public class BindingStore(private val file: Path) {
  /**
   * Writes the bindings of [contexts], creating the folder when needed. The file is written beside
   * its final place and moved over it, so a crash never leaves half a file. Context names must
   * differ.
   */
  public fun save(contexts: Collection<InputContext>) {
    require(contexts.map { it.name }.toSet().size == contexts.size) { "Context names must differ" }
    val byContext = contexts.associate { context -> context.name to bindingsToJson(context) }
    val root =
      JsonObject(mapOf("version" to JsonPrimitive(VERSION), "contexts" to JsonObject(byContext)))
    val folder = file.toAbsolutePath().parent
    Files.createDirectories(folder)
    val temporary = Files.createTempFile(folder, file.fileName.toString(), ".tmp")
    Files.writeString(temporary, CanonicalYaml.write(root))
    Files.move(
      temporary,
      file,
      StandardCopyOption.ATOMIC_MOVE,
      StandardCopyOption.REPLACE_EXISTING,
    )
  }

  /**
   * Replaces the bindings of each of [contexts] that the file has an entry for, and leaves the rest
   * as they are. The whole file is checked before any context changes: every control must be one a
   * device reads (see [Controls]) and every action one of [actions]. Answers false when there is no
   * file. Throws [IllegalArgumentException] when the file is not a valid bindings file.
   */
  public fun load(contexts: Collection<InputContext>, actions: ActionSet): Boolean {
    if (!Files.exists(file)) return false
    val root = CanonicalYaml.read(Files.readString(file)).jsonObject
    val version = root["version"]?.jsonPrimitive?.contentOrNull
    require(version == VERSION.toString()) { "$file has bindings version $version, not $VERSION" }
    val saved = root["contexts"]?.jsonObject ?: JsonObject(emptyMap())
    val parsed = contexts.mapNotNull { context ->
      saved[context.name]?.let { context to bindingsFromJson(it.jsonArray, actions) }
    }
    for ((context, bindings) in parsed) context.replaceBindings(bindings)
    return true
  }

  private fun bindingsToJson(context: InputContext): JsonElement =
    JsonArray(
      context.bindings.map {
        JsonObject(
          mapOf(
            "action" to JsonPrimitive(it.action),
            "control" to JsonPrimitive(it.control),
            "scale" to JsonPrimitive(it.scale),
          )
        )
      }
    )

  private fun bindingsFromJson(array: JsonArray, actions: ActionSet): List<Binding> =
    array.map { element ->
      val entry = element.jsonObject
      val action =
        requireNotNull(entry["action"]?.jsonPrimitive?.contentOrNull) { "A binding has no action" }
      val control =
        requireNotNull(entry["control"]?.jsonPrimitive?.contentOrNull) {
          "A binding has no control"
        }
      require(Controls.isKnown(control)) { "Unknown control $control" }
      require(actions.indexOf(action) >= 0) { "Unknown action $action" }
      Binding(action, control, entry["scale"]?.jsonPrimitive?.floatOrNull ?: 1f)
    }
}

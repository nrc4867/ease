package dev.chieppa.ease.importers.gltf

import dev.chieppa.ease.animation.assets.ClipEvent
import dev.chieppa.ease.assets.importing.ImportException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.floatOrNull

/**
 * What a glTF file's import settings say about its clips, since glTF itself has no animation
 * events: under `clips`, each clip's cooked name without its extension (the animation's name, made
 * a file name, or `clip3` for an unnamed fourth animation) maps to its settings, whose `events`
 * list each event's `name` and `time` in seconds from the clip's start:
 * ```yaml
 * settings:
 *   clips:
 *     Walk:
 *       events:
 *         - name: footstepLeft
 *           time: 0.03
 * ```
 *
 * Events come out sorted by time, then name. A setting naming a clip the file lacks, or an event
 * without a name, without a time or past the clip's end, fails the import, naming the setting.
 */
internal class ClipSettings(settings: JsonObject) {
  private val clips: Map<String, List<SetEvent>> = read(settings)

  /**
   * The events set for the clip named [name], [duration] seconds long; none when it has no
   * settings. Fails when an event's time is past the clip's end.
   */
  fun events(name: String, duration: Float): List<ClipEvent> =
    clips[name].orEmpty().map { set ->
      if (set.event.time > duration) {
        fail("${set.path}.time", "is ${set.event.time} s, past the clip's end at $duration s")
      }
      set.event
    }

  /** Fails when a setting names a clip not among [cooked]. */
  fun checkNames(cooked: List<String>) {
    val unknown = clips.keys - cooked.toSet()
    if (unknown.isNotEmpty()) {
      throw ImportException("settings.clips names clips the file lacks: ${unknown.sorted()}")
    }
  }

  private fun read(settings: JsonObject): Map<String, List<SetEvent>> {
    val clips = settings["clips"] ?: return emptyMap()
    val byName = clips as? JsonObject ?: fail("settings.clips", "is not a map of animation names")
    return byName.mapValues { (name, clip) -> events(clip, "settings.clips.$name") }
  }

  private fun events(clip: JsonElement, path: String): List<SetEvent> {
    val fields = clip as? JsonObject ?: fail(path, "is not a map")
    val list = fields["events"] ?: return emptyList()
    val events = list as? JsonArray ?: fail("$path.events", "is not a list")
    return events
      .mapIndexed { index, element ->
        val at = "$path.events[$index]"
        val event = element as? JsonObject ?: fail(at, "is not a map")
        val name = (event["name"] as? JsonPrimitive)?.takeIf { it.isString }?.content
        val time = (event["time"] as? JsonPrimitive)?.floatOrNull
        if (name.isNullOrEmpty()) fail("$at.name", "is missing")
        if (time == null || time < 0f) fail("$at.time", "is not a time in seconds")
        SetEvent(ClipEvent(name, time), at)
      }
      .sortedWith(compareBy({ it.event.time }, { it.event.name }))
  }

  /** An event as set, with the path of its setting. */
  private class SetEvent(val event: ClipEvent, val path: String)

  private fun fail(path: String, problem: String): Nothing = throw ImportException("$path $problem")
}

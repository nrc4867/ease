package dev.chieppa.ease.gltf

import dev.chieppa.ease.gltf.json.GltfRoot
import dev.chieppa.ease.gltf.json.NodeJson
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull

/**
 * A `KHR_lights_punctual` light: its [type] (`directional`, `point` or `spot`), linear [color] and
 * [intensity] (lux for a directional light, candela otherwise). It shines down its node's -z.
 */
public class PunctualLight(
  public val name: String?,
  public val type: String,
  public val color: FloatArray,
  public val intensity: Float,
)

/** Reads the `KHR_lights_punctual` extension from a file's root and nodes. */
internal object PunctualLights {
  const val EXTENSION = "KHR_lights_punctual"
  private const val CHANNELS = 3

  fun of(root: GltfRoot): List<PunctualLight> {
    val lights = (root.extensions?.get(EXTENSION) as? JsonObject)?.get("lights") as? JsonArray
    return lights.orEmpty().mapIndexed { index, element ->
      val path = "extensions.$EXTENSION.lights[$index]"
      val light = element as? JsonObject ?: fail(path, "is not an object")
      val type = (light["type"] as? JsonPrimitive)?.content ?: fail(path, "has no type")
      val color = (light["color"] as? JsonArray)?.map { number(it, path) } ?: listOf(1f, 1f, 1f)
      if (color.size != CHANNELS) fail("$path.color", "needs 3 numbers")
      val intensity = light["intensity"]?.let { number(it, path) } ?: 1f
      PunctualLight(
        (light["name"] as? JsonPrimitive)?.content,
        type,
        color.toFloatArray(),
        intensity,
      )
    }
  }

  /** The index of the light [node] carries, or null for none. */
  fun indexOf(node: NodeJson, path: String): Int? {
    val extension = node.extensions?.get(EXTENSION) as? JsonObject ?: return null
    return (extension["light"] as? JsonPrimitive)?.intOrNull
      ?: fail("$path.extensions.$EXTENSION", "has no light index")
  }

  private fun number(element: kotlinx.serialization.json.JsonElement, path: String): Float =
    (element as? JsonPrimitive)?.floatOrNull ?: fail(path, "$element is not a number")
}

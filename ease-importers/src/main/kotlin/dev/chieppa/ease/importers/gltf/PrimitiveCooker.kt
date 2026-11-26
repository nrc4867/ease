package dev.chieppa.ease.importers.gltf

import dev.chieppa.ease.gltf.GltfFile
import dev.chieppa.ease.gltf.fail
import dev.chieppa.ease.gltf.json.PrimitiveJson
import dev.chieppa.ease.render.assets.MeshStreams

private const val VEC2 = 2
private const val VEC3 = 3
private const val VEC4 = 4
private const val TRIANGLE = 3

/**
 * Reads one glTF primitive into the streams of a cooked mesh: positions, normals (worked out from
 * the faces when the file has none), first texture coordinates (zero when absent), tangents (worked
 * out from the coordinates when absent), indices (0 to n - 1 when the file has none), and the
 * optional second coordinates, colors, joints and weights. Only triangle lists are read; any other
 * mode is an error naming the primitive.
 */
internal class PrimitiveCooker(private val file: GltfFile) {
  /** The streams of primitive [index] of mesh [mesh]. */
  fun cook(mesh: Int, index: Int): MeshStreams {
    val path = "meshes[$mesh].primitives[$index]"
    val primitive = file.root.meshes[mesh].primitives[index]
    if (primitive.mode != PrimitiveJson.TRIANGLES) {
      fail("$path.mode", "mode ${primitive.mode} is not a triangle list (4)")
    }
    val attributes = "$path.attributes"
    val positionAccessor = primitive.attributes["POSITION"] ?: fail(attributes, "has no POSITION")
    val positions = floats(positionAccessor, "$attributes.POSITION", VEC3)
    val count = positions.size / VEC3
    val indices = indices(primitive, path, count)
    val normals =
      attribute(primitive, "NORMAL", attributes, VEC3, count)
        ?: Tangents.normals(positions, indices)
    val uv0 =
      attribute(primitive, "TEXCOORD_0", attributes, VEC2, count) ?: FloatArray(count * VEC2)
    val tangents =
      attribute(primitive, "TANGENT", attributes, VEC4, count)
        ?: Tangents.fromUv(positions, normals, uv0, indices)
    return MeshStreams(
      positions,
      normals,
      uv0,
      tangents,
      indices,
      joints(primitive, attributes, count),
      attribute(primitive, "WEIGHTS_0", attributes, VEC4, count),
      attribute(primitive, "TEXCOORD_1", attributes, VEC2, count),
      colors(primitive, attributes, count),
    )
  }

  private fun indices(primitive: PrimitiveJson, path: String, count: Int): IntArray {
    val accessor = primitive.indices
    if (accessor == null) {
      if (count % TRIANGLE != 0) {
        fail("$path.attributes.POSITION", "$count vertices without indices are not whole triangles")
      }
      return IntArray(count) { it }
    }
    val values = file.accessors.ints(accessor, "$path.indices").values
    if (values.size % TRIANGLE != 0) {
      fail("$path.indices", "${values.size} indices are not whole triangles")
    }
    values
      .firstOrNull { it !in 0 until count }
      ?.let {
        fail("$path.indices", "index $it is past the $count vertices")
      }
    return values
  }

  private fun attribute(
    primitive: PrimitiveJson,
    name: String,
    attributes: String,
    components: Int,
    count: Int,
  ): FloatArray? {
    val accessor = primitive.attributes[name] ?: return null
    val values = floats(accessor, "$attributes.$name", components)
    if (values.size != count * components) {
      fail(
        "$attributes.$name",
        "has ${values.size / components} elements, not $count",
      )
    }
    return values
  }

  private fun joints(primitive: PrimitiveJson, attributes: String, count: Int): IntArray? {
    val accessor = primitive.attributes["JOINTS_0"] ?: return null
    val joints = file.accessors.ints(accessor, "$attributes.JOINTS_0")
    if (joints.components != VEC4 || joints.count != count) {
      fail("$attributes.JOINTS_0", "needs $count VEC4 elements")
    }
    return joints.values
  }

  /** COLOR_0 as linear RGBA; a VEC3 color gets alpha 1. */
  private fun colors(primitive: PrimitiveJson, attributes: String, count: Int): FloatArray? {
    val accessor = primitive.attributes["COLOR_0"] ?: return null
    val colors = file.accessors.floats(accessor, "$attributes.COLOR_0")
    if (colors.count != count || colors.components !in VEC3..VEC4) {
      fail("$attributes.COLOR_0", "needs $count VEC3 or VEC4 elements")
    }
    return FloatArray(count * VEC4) { at ->
      val component = at % VEC4
      if (component < colors.components) colors[at / VEC4, component] else 1f
    }
  }

  private fun floats(accessor: Int, path: String, components: Int): FloatArray {
    val values = file.accessors.floats(accessor, path)
    if (values.components != components) {
      fail(path, "has ${values.components} components, not $components")
    }
    return values.values
  }
}

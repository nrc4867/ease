package dev.chieppa.ease.gltf

import dev.chieppa.ease.gltf.json.AccessorJson
import dev.chieppa.ease.gltf.json.GltfRoot
import dev.chieppa.ease.memory.MappedRegion

/**
 * Reads a file's accessors through its buffer views: every component type, normalized integers,
 * interleaved strides, matrices with aligned columns, accessors with no buffer view, and sparse
 * replacements. Every failure names the accessor, and the element that used it when one is given.
 */
public class Accessors
internal constructor(
  private val root: GltfRoot,
  private val views: BufferViews,
) {
  /** The accessor [index] as floats; [usedBy] names what reads it, for errors. */
  public fun floats(index: Int, usedBy: String? = null): FloatValues {
    val accessor = accessor(index, usedBy)
    val path = path(index, usedBy)
    val out = FloatArray(accessor.count * elementLayout(accessor, path).components)
    read(accessor, path) { at, region, offset, type ->
      out[at] = ComponentTypes.float(region, offset, type, accessor.normalized)
    }
    return FloatValues(accessor.count, elementLayout(accessor, path).components, out)
  }

  /** The integer accessor [index], such as indices or joints; a float accessor is refused. */
  public fun ints(index: Int, usedBy: String? = null): IntValues {
    val accessor = accessor(index, usedBy)
    val path = path(index, usedBy)
    if (!ComponentTypes.isInteger(accessor.componentType) || accessor.normalized) {
      fail(path, "holds ${accessor.componentType} numbers, not plain integers")
    }
    val components = elementLayout(accessor, path).components
    val out = IntArray(accessor.count * components)
    read(accessor, path) { at, region, offset, type ->
      out[at] = ComponentTypes.raw(region, offset, type).toLong().toInt()
    }
    return IntValues(accessor.count, components, out)
  }

  /** The accessor [index] as the file declares it. */
  public fun accessor(index: Int, usedBy: String? = null): AccessorJson =
    root.accessors.getOrNull(index)
      ?: fail(
        usedBy ?: "accessors",
        "names accessor $index, but the file has ${root.accessors.size}",
      )

  private fun path(index: Int, usedBy: String?): String =
    if (usedBy == null) "accessors[$index]" else "$usedBy: accessors[$index]"

  private fun elementLayout(accessor: AccessorJson, path: String): ElementLayout {
    val size =
      ComponentTypes.size(accessor.componentType)
        ?: fail(path, "componentType ${accessor.componentType} is not glTF's")
    return ElementLayout(accessor.type, size, path)
  }

  private inline fun read(
    accessor: AccessorJson,
    path: String,
    store: (Int, MappedRegion, Long, Int) -> Unit,
  ) {
    if (accessor.count < 0) fail(path, "count ${accessor.count} is negative")
    val layout = elementLayout(accessor, path)
    val type = accessor.componentType
    accessor.bufferView?.let { view ->
      val stride = views.stride(view, layout.byteSize)
      val needed = if (accessor.count == 0) 0 else stride * (accessor.count - 1L) + layout.byteSize
      val region = views.slice(view, accessor.byteOffset, needed, path)
      for (element in 0 until accessor.count) {
        for (component in 0 until layout.components) {
          val offset = element.toLong() * stride + layout.offsetOf(component)
          store(element * layout.components + component, region, offset, type)
        }
      }
    }
    accessor.sparse?.let { sparse ->
      val sparsePath = "$path.sparse"
      val indexSize =
        ComponentTypes.size(sparse.indices.componentType)?.takeIf {
          ComponentTypes.isInteger(sparse.indices.componentType)
        } ?: fail("$sparsePath.indices", "componentType is not an integer type")
      val indices =
        views.slice(
          sparse.indices.bufferView,
          sparse.indices.byteOffset,
          sparse.count.toLong() * indexSize,
          "$sparsePath.indices",
        )
      val values =
        views.slice(
          sparse.values.bufferView,
          sparse.values.byteOffset,
          sparse.count.toLong() * layout.byteSize,
          "$sparsePath.values",
        )
      for (entry in 0 until sparse.count) {
        val target =
          ComponentTypes.raw(indices, entry.toLong() * indexSize, sparse.indices.componentType)
            .toLong()
        if (target !in 0 until accessor.count) {
          fail(
            "$sparsePath.indices",
            "index $target is past count ${accessor.count}",
          )
        }
        for (component in 0 until layout.components) {
          val offset = entry.toLong() * layout.byteSize + layout.offsetOf(component)
          store(target.toInt() * layout.components + component, values, offset, type)
        }
      }
    }
  }
}

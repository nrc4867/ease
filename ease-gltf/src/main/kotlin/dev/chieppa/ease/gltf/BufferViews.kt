package dev.chieppa.ease.gltf

import dev.chieppa.ease.gltf.json.GltfRoot
import dev.chieppa.ease.memory.MappedRegion

/** A file's buffer views over its loaded buffers, each slice checked against both. */
internal class BufferViews(private val root: GltfRoot, private val buffers: List<MappedRegion>) {
  /** The bytes from [offset] to [offset] + [length] of view [index], for the element [path]. */
  fun slice(index: Int, offset: Long, length: Long, path: String): MappedRegion {
    val view =
      root.bufferViews.getOrNull(index)
        ?: fail(
          path,
          "names buffer view $index, but the file has ${root.bufferViews.size}",
        )
    if (offset < 0 || offset + length > view.byteLength) {
      fail(
        path,
        "$length bytes at offset $offset run past the ${view.byteLength} of bufferViews[$index]",
      )
    }
    return whole(index).region(offset, length)
  }

  /** All of view [index]. */
  fun whole(index: Int): MappedRegion {
    val path = "bufferViews[$index]"
    val view =
      root.bufferViews.getOrNull(index)
        ?: fail(path, "the file has ${root.bufferViews.size} buffer views")
    val buffer =
      buffers.getOrNull(view.buffer)
        ?: fail(
          path,
          "names buffer ${view.buffer}, but the file has ${buffers.size}",
        )
    if (view.byteOffset < 0 || view.byteOffset + view.byteLength > buffer.byteSize) {
      fail(
        path,
        "${view.byteLength} bytes at offset ${view.byteOffset} run past the ${buffer.byteSize} " +
          "bytes of buffers[${view.buffer}]",
      )
    }
    return buffer.region(view.byteOffset, view.byteLength)
  }

  /** Bytes between elements in view [index]: its stride, or [packed] when it has none. */
  fun stride(index: Int, packed: Int): Int = root.bufferViews.getOrNull(index)?.byteStride ?: packed
}

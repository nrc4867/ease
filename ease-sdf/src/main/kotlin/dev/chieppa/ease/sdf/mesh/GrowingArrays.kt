package dev.chieppa.ease.sdf.mesh

/** Floats appended without boxing, for building meshes. */
internal class FloatList {
  private var values = FloatArray(INITIAL)

  var size: Int = 0
    private set

  fun add(more: FloatArray) {
    if (size + more.size > values.size)
      values = values.copyOf(maxOf(values.size * 2, size + more.size))
    more.copyInto(values, size)
    size += more.size
  }

  fun toArray(): FloatArray = values.copyOf(size)
}

/** Ints appended without boxing, for building meshes. */
internal class IntList {
  private var values = IntArray(INITIAL)

  var size: Int = 0
    private set

  fun add(more: IntArray) {
    if (size + more.size > values.size)
      values = values.copyOf(maxOf(values.size * 2, size + more.size))
    more.copyInto(values, size)
    size += more.size
  }

  fun toArray(): IntArray = values.copyOf(size)
}

private const val INITIAL = 1024

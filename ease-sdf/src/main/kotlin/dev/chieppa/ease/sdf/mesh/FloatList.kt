package dev.chieppa.ease.sdf.mesh

/** Floats appended without boxing, for building meshes. */
internal class FloatList {
  private var values = FloatArray(INITIAL_ROOM)

  var size: Int = 0
    private set

  fun add(more: FloatArray) {
    val needed = size + more.size
    if (needed > values.size) values = values.copyOf(grownLength(values.size, needed))
    more.copyInto(values, size)
    size = needed
  }

  fun toArray(): FloatArray = values.copyOf(size)
}

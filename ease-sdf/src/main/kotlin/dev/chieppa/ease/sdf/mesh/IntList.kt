package dev.chieppa.ease.sdf.mesh

/** Ints appended without boxing, for building meshes. */
internal class IntList {
  private var values = IntArray(INITIAL_ROOM)

  var size: Int = 0
    private set

  fun add(more: IntArray) {
    val needed = size + more.size
    if (needed > values.size) values = values.copyOf(grownLength(values.size, needed))
    more.copyInto(values, size)
    size = needed
  }

  fun toArray(): IntArray = values.copyOf(size)
}

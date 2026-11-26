package dev.chieppa.ease.gltf

private const val COLUMN_ALIGNMENT = 4
private const val THREE = 3
private const val FOUR = 4

/**
 * How one element of an accessor is laid out: [components] numbers of [componentSize] bytes, in
 * [columns] columns, each column starting on a 4-byte boundary as glTF requires of matrices.
 */
internal class ElementLayout(type: String, val componentSize: Int, element: String) {
  /** The numbers in one element. */
  val components: Int

  /** Columns: the matrix's for `MAT2` to `MAT4`, otherwise 1. */
  val columns: Int

  init {
    val (rows, columnCount) =
      when (type) {
        "SCALAR" -> 1 to 1
        "VEC2" -> 2 to 1
        "VEC3" -> THREE to 1
        "VEC4" -> FOUR to 1
        "MAT2" -> 2 to 2
        "MAT3" -> THREE to THREE
        "MAT4" -> FOUR to FOUR
        else -> fail(element, "type '$type' is not a glTF accessor type")
      }
    components = rows * columnCount
    columns = columnCount
  }

  private val rows = components / columns

  /** Bytes from one column's start to the next. */
  val columnStride: Int = align(rows * componentSize)

  /** Bytes one element takes when elements are packed. */
  val byteSize: Int = if (columns == 1) rows * componentSize else columnStride * columns

  /** Byte offset of number [index] of an element from the element's start. */
  fun offsetOf(index: Int): Int = (index / rows) * columnStride + (index % rows) * componentSize

  private fun align(bytes: Int): Int =
    if (columns == 1) bytes
    else (bytes + COLUMN_ALIGNMENT - 1) / COLUMN_ALIGNMENT * COLUMN_ALIGNMENT
}

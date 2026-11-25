package dev.chieppa.ease.codegen.model

/** The alignment WGSL's uniform address space asks of a nested struct and the field after it. */
private const val UNIFORM_STRUCT_ALIGNMENT = 16L

/**
 * A [GpuStructModel] laid out under WGSL's rules: each field's offset, the struct's size and
 * alignment, and the `@align` and `@size` attributes a nested struct field takes so the struct is
 * valid in the uniform address space as well as storage, with the same offsets in both.
 */
internal data class GpuStructLayout(
  val model: GpuStructModel,
  val fields: List<GpuFieldLayout>,
  /** WGSL's `AlignOf`: the largest alignment of any field. */
  val alignment: Long,
  /** WGSL's `SizeOf`: past the last field, rounded up to [alignment]. */
  val size: Long,
) {
  companion object {
    /** Lays out [model]; every field's type must be supported. */
    fun of(model: GpuStructModel): GpuStructLayout {
      var end = 0L
      var alignment = 1L
      val fields =
        model.fields.map { field ->
          val placed = place(field)
          val offset = roundUp(end, placed.alignment)
          end = offset + placed.size
          alignment = maxOf(alignment, placed.alignment)
          placed.copy(offset = offset)
        }
      return GpuStructLayout(model, fields, alignment, roundUp(end, alignment))
    }

    private fun place(field: GpuFieldModel): GpuFieldLayout =
      when (val type = field.type) {
        is GpuFieldType.Plain ->
          GpuFieldLayout(field, 0, type.kind.size, type.kind.alignment, type.kind.wgsl)
        is GpuFieldType.Struct -> {
          val inner = of(type.model)
          val alignment = roundUp(inner.alignment, UNIFORM_STRUCT_ALIGNMENT)
          val size = roundUp(inner.size, UNIFORM_STRUCT_ALIGNMENT)
          GpuFieldLayout(
            field,
            0,
            size,
            alignment,
            type.model.wgslName,
            alignAttribute = alignment.takeIf { it != inner.alignment },
            sizeAttribute = size.takeIf { it != inner.size },
          )
        }
        is GpuFieldType.Unsupported -> error("${field.name} has no GPU layout: ${type.typeName}")
      }

    private fun roundUp(value: Long, alignment: Long): Long =
      (value + alignment - 1) / alignment * alignment
  }
}

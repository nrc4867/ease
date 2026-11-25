package dev.chieppa.ease.memory

/**
 * The layout of a struct the GPU reads, as ease-codegen writes it for a [GpuStruct] class: its WGSL
 * name and declaration, its size and alignment under WGSL's rules, and the structs it nests.
 */
public interface GpuLayout {
  /** The struct's name in WGSL. */
  public val wgslName: String

  /** The struct's WGSL declaration. */
  public val wgsl: String

  /** WGSL's `SizeOf` the struct: the bytes one takes, and the stride of an array of them. */
  public val byteSize: Long

  /** WGSL's `AlignOf` the struct. */
  public val alignment: Long

  /** The structs this one has as fields, each once. */
  public val nested: List<GpuLayout>
}

/**
 * The WGSL declarations of [layouts] and every struct they nest, each once, a nested struct before
 * the structs holding it, for a shader to start with.
 */
public fun wgslDeclarations(layouts: List<GpuLayout>): String {
  val ordered = LinkedHashMap<String, GpuLayout>()
  fun visit(layout: GpuLayout) {
    if (layout.wgslName in ordered) return
    layout.nested.forEach(::visit)
    ordered[layout.wgslName] = layout
  }
  layouts.forEach(::visit)
  return ordered.values.joinToString("\n") { it.wgsl }
}

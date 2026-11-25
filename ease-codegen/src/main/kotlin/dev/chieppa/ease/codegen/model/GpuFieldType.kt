package dev.chieppa.ease.codegen.model

/** What a field of a GPU struct is in WGSL. */
internal sealed interface GpuFieldType {
  /** A scalar, vector or matrix. */
  data class Plain(val kind: GpuPrimitive) : GpuFieldType

  /** Another `@GpuStruct` struct, nested by value. */
  data class Struct(val model: GpuStructModel) : GpuFieldType

  /** A Kotlin type a GPU struct cannot hold, named as written. */
  data class Unsupported(val typeName: String) : GpuFieldType
}

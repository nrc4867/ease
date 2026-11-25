package dev.chieppa.ease.codegen.model

/**
 * A `@GpuStruct` class as the processor reads it: where it lives, how far it can be seen, and its
 * fields in declaration order.
 */
internal data class GpuStructModel(
  val packageName: String,
  /** The class's name inside its package, outer classes first, joined by dots. */
  val className: String,
  val fields: List<GpuFieldModel>,
  val visibility: ClassVisibility = ClassVisibility.PUBLIC,
) {
  /** The struct's name in WGSL: the class's own simple name. */
  val wgslName: String
    get() = className.substringAfterLast('.')

  /** The generated writer object's name: the class path joined by underscores, then `Writer`. */
  val writerName: String
    get() = className.replace('.', '_') + "Writer"

  /** The writer's qualified name, for structs that nest this one. */
  val qualifiedWriterName: String
    get() = if (packageName.isEmpty()) writerName else "$packageName.$writerName"
}

/** One field of a [GpuStructModel]: its Kotlin name and what it is on the GPU. */
internal data class GpuFieldModel(val name: String, val type: GpuFieldType)

/** What a field of a GPU struct is in WGSL. */
internal sealed interface GpuFieldType {
  /** A scalar, vector or matrix. */
  data class Plain(val kind: GpuPrimitive) : GpuFieldType

  /** Another `@GpuStruct` struct, nested by value. */
  data class Struct(val model: GpuStructModel) : GpuFieldType

  /** A Kotlin type a GPU struct cannot hold, named as written. */
  data class Unsupported(val typeName: String) : GpuFieldType
}

/** The WGSL types a `@GpuStruct` field maps to, with their size and alignment from the spec. */
@Suppress("MagicNumber") // Byte sizes and alignments, as WGSL's memory layout table gives them.
internal enum class GpuPrimitive(val wgsl: String, val size: Long, val alignment: Long) {
  F32("f32", 4, 4),
  I32("i32", 4, 4),
  U32("u32", 4, 4),
  VEC2("vec2f", 8, 8),
  VEC3("vec3f", 12, 16),
  VEC4("vec4f", 16, 16),
  MAT4("mat4x4f", 64, 16);

  companion object {
    private const val MATH = "dev.chieppa.ease.math"

    /** The primitive a Kotlin type maps to, by its qualified name, or null for none. */
    fun of(typeName: String): GpuPrimitive? =
      when (typeName) {
        "kotlin.Float" -> F32
        "kotlin.Int" -> I32
        "kotlin.UInt" -> U32
        "$MATH.Vec2" -> VEC2
        "$MATH.Vec3" -> VEC3
        "$MATH.Vec4" -> VEC4
        "$MATH.Mat4" -> MAT4
        else -> null
      }
  }
}

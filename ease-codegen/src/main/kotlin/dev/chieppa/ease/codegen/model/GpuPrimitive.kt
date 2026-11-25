package dev.chieppa.ease.codegen.model

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

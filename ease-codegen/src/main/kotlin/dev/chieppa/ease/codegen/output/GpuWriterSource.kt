package dev.chieppa.ease.codegen.output

import dev.chieppa.ease.codegen.model.ClassVisibility
import dev.chieppa.ease.codegen.model.GpuFieldLayout
import dev.chieppa.ease.codegen.model.GpuFieldType
import dev.chieppa.ease.codegen.model.GpuPrimitive
import dev.chieppa.ease.codegen.model.GpuStructLayout

/**
 * The Kotlin source of a GPU struct's writer: an object in the class's package holding each field's
 * offset as a constant, the struct's size, alignment and WGSL declaration, and one function per
 * field writing it through a `NativeWriter` at that offset from the writer's base.
 */
internal object GpuWriterSource {
  private const val SIZE = "STRUCT_SIZE"
  private const val ALIGNMENT = "STRUCT_ALIGNMENT"
  private const val WGSL = "STRUCT_WGSL"
  private val WORD_BREAK = Regex("([a-z0-9])([A-Z])")

  /** Constant names the writer keeps for the struct itself, which no field's offset may take. */
  val RESERVED_CONSTANTS: Set<String> = setOf(SIZE, ALIGNMENT, WGSL)

  /** The constant holding a field's offset: its name in upper snake case. */
  fun constantName(field: String): String = field.replace(WORD_BREAK, "$1_$2").uppercase()

  fun of(layout: GpuStructLayout): String = buildString {
    val model = layout.model
    val visibility = if (model.visibility == ClassVisibility.INTERNAL) "internal" else "public"
    appendLine("// Written by ease-codegen from ${model.className}; changes here are lost.")
    if (model.packageName.isNotEmpty()) {
      appendLine("package ${model.packageName}")
      appendLine()
    }
    appendLine("import dev.chieppa.ease.memory.GpuLayout")
    appendLine("import dev.chieppa.ease.memory.NativeWriter")
    appendLine()
    appendLine("/** Writes [${model.className}] structs, laid out under WGSL's rules. */")
    appendLine("$visibility object ${model.writerName} : GpuLayout {")
    layout.fields.forEach { field ->
      appendLine("  /** Byte offset of `${field.field.name}`. */")
      appendLine("  public const val ${constantName(field.field.name)}: Long = ${field.offset}L")
    }
    appendLine("  /** Bytes one struct takes, and the stride of an array of them. */")
    appendLine("  public const val $SIZE: Long = ${layout.size}L")
    appendLine("  /** The struct's alignment. */")
    appendLine("  public const val $ALIGNMENT: Long = ${layout.alignment}L")
    appendLine("  /** The struct's WGSL declaration. */")
    appendLine("  public const val $WGSL: String = ${quoted(WgslStructSource.of(layout))}")
    appendLine("  override val wgslName: String = \"${model.wgslName}\"")
    appendLine("  override val wgsl: String = $WGSL")
    appendLine("  override val byteSize: Long = $SIZE")
    appendLine("  override val alignment: Long = $ALIGNMENT")
    appendLine("  override val nested: List<GpuLayout> = listOf(${nestedWriters(layout)})")
    layout.fields.forEach { appendField(it) }
    appendLine("}")
  }

  private fun StringBuilder.appendField(field: GpuFieldLayout) {
    val name = field.field.name
    val offset = constantName(name)
    appendLine()
    when (val type = field.field.type) {
      is GpuFieldType.Plain -> {
        appendLine("  /** Writes `$name`. */")
        appendLine("  public fun $name(writer: NativeWriter, ${parameters(type.kind)}) {")
        appendLine("    writer.${call(type.kind, offset)}")
        appendLine("  }")
      }
      is GpuFieldType.Struct -> {
        appendLine("  /** Runs [write] with the writer's base at `$name`, a nested struct. */")
        appendLine("  public inline fun $name(writer: NativeWriter, write: () -> Unit) {")
        appendLine("    writer.nested($offset, write)")
        appendLine("  }")
      }
      is GpuFieldType.Unsupported -> error("$name cannot be written")
    }
  }

  private fun parameters(kind: GpuPrimitive): String =
    when (kind) {
      GpuPrimitive.F32 -> "value: Float"
      GpuPrimitive.I32 -> "value: Int"
      GpuPrimitive.U32 -> "value: UInt"
      GpuPrimitive.VEC2 -> "x: Float, y: Float"
      GpuPrimitive.VEC3 -> "x: Float, y: Float, z: Float"
      GpuPrimitive.VEC4 -> "x: Float, y: Float, z: Float, w: Float"
      GpuPrimitive.MAT4 -> "matrix: FloatArray, from: Int = 0"
    }

  private fun call(kind: GpuPrimitive, offset: String): String =
    when (kind) {
      GpuPrimitive.F32 -> "f32($offset, value)"
      GpuPrimitive.I32 -> "i32($offset, value)"
      GpuPrimitive.U32 -> "u32($offset, value)"
      GpuPrimitive.VEC2 -> "vec2($offset, x, y)"
      GpuPrimitive.VEC3 -> "vec3($offset, x, y, z)"
      GpuPrimitive.VEC4 -> "vec4($offset, x, y, z, w)"
      GpuPrimitive.MAT4 -> "mat4($offset, matrix, from)"
    }

  private fun nestedWriters(layout: GpuStructLayout): String =
    layout.fields
      .mapNotNull { (it.field.type as? GpuFieldType.Struct)?.model?.qualifiedWriterName }
      .distinct()
      .joinToString()

  private fun quoted(text: String): String =
    "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\""
}

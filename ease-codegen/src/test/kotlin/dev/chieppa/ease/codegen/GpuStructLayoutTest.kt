package dev.chieppa.ease.codegen

import dev.chieppa.ease.codegen.model.GpuFieldModel
import dev.chieppa.ease.codegen.model.GpuFieldType
import dev.chieppa.ease.codegen.model.GpuPrimitive
import dev.chieppa.ease.codegen.model.GpuStructLayout
import dev.chieppa.ease.codegen.model.GpuStructModel
import dev.chieppa.ease.codegen.output.GpuWriterSource
import dev.chieppa.ease.codegen.output.WgslStructSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Offsets, sizes and attributes as WGSL's alignment rules give them. */
class GpuStructLayoutTest {
  private fun plain(name: String, kind: GpuPrimitive) =
    GpuFieldModel(name, GpuFieldType.Plain(kind))

  private fun struct(name: String, vararg fields: GpuFieldModel) =
    GpuStructModel("test", name, fields.toList())

  private fun offsets(layout: GpuStructLayout) = layout.fields.map { it.offset }

  @Test
  fun `a scalar after a vec3 fills its fourth slot, and a vec3 after a scalar starts at 16`() {
    val packed =
      GpuStructLayout.of(struct("A", plain("v", GpuPrimitive.VEC3), plain("s", GpuPrimitive.F32)))
    val padded =
      GpuStructLayout.of(struct("B", plain("s", GpuPrimitive.F32), plain("v", GpuPrimitive.VEC3)))

    assertEquals(listOf(0L, 12L), offsets(packed))
    assertEquals(16L, packed.size)
    assertEquals(listOf(0L, 16L), offsets(padded))
    assertEquals(32L, padded.size)
  }

  @Test
  fun `vec2 aligns to 8, a matrix to 16, and the size rounds up to the largest alignment`() {
    val layout =
      GpuStructLayout.of(
        struct(
          "C",
          plain("a", GpuPrimitive.F32),
          plain("b", GpuPrimitive.VEC2),
          plain("c", GpuPrimitive.U32),
          plain("m", GpuPrimitive.MAT4),
          plain("d", GpuPrimitive.I32),
        )
      )

    assertEquals(listOf(0L, 8L, 16L, 32L, 96L), offsets(layout))
    assertEquals(16L, layout.alignment)
    assertEquals(112L, layout.size)
  }

  @Test
  fun `a nested struct is aligned and sized to 16 for the uniform space, with attributes`() {
    val small = struct("Small", plain("a", GpuPrimitive.F32), plain("b", GpuPrimitive.F32))
    val outer =
      struct(
        "Outer",
        plain("x", GpuPrimitive.F32),
        GpuFieldModel("inner", GpuFieldType.Struct(small)),
        plain("y", GpuPrimitive.F32),
      )

    val layout = GpuStructLayout.of(outer)

    assertEquals(listOf(0L, 16L, 32L), offsets(layout))
    assertEquals(48L, layout.size)
    assertTrue("  @align(16) @size(16) inner: Small," in WgslStructSource.of(layout))
  }

  @Test
  fun `the writer holds offsets as constants and the WGSL declaration as text`() {
    val layout =
      GpuStructLayout.of(
        struct("View", plain("viewProjection", GpuPrimitive.MAT4), plain("eye", GpuPrimitive.VEC3))
      )

    val source = GpuWriterSource.of(layout)

    assertTrue("public const val VIEW_PROJECTION: Long = 0L" in source, source)
    assertTrue("public const val EYE: Long = 64L" in source, source)
    assertTrue("public const val STRUCT_SIZE: Long = 80L" in source, source)
    assertTrue("struct View {\\n  viewProjection: mat4x4f,\\n  eye: vec3f,\\n}" in source, source)
    assertTrue("writer.vec3(EYE, x, y, z)" in source, source)
  }
}

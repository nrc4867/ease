package dev.chieppa.ease.codegen

import dev.chieppa.ease.codegen.model.ClassVisibility
import dev.chieppa.ease.codegen.model.GpuFieldModel
import dev.chieppa.ease.codegen.model.GpuFieldType
import dev.chieppa.ease.codegen.model.GpuPrimitive
import dev.chieppa.ease.codegen.model.GpuStructModel
import dev.chieppa.ease.codegen.rules.GpuStructRules
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** What keeps a GPU struct from being laid out. */
class GpuStructRulesTest {
  private val f32 = GpuFieldType.Plain(GpuPrimitive.F32)

  @Test
  fun `a supported struct has no problems`() {
    val model = GpuStructModel("test", "Light", listOf(GpuFieldModel("power", f32)))

    assertEquals(emptyList(), GpuStructRules.problems(model))
  }

  @Test
  fun `unsupported types, reserved names, hidden classes and empty structs are problems`() {
    val model =
      GpuStructModel(
        "test",
        "Bad",
        listOf(
          GpuFieldModel("label", GpuFieldType.Unsupported("kotlin.String")),
          GpuFieldModel("structSize", f32),
        ),
        ClassVisibility.HIDDEN,
      )
    val empty = GpuStructModel("test", "Empty", emptyList())

    val problems = GpuStructRules.problems(model)

    assertEquals(listOf(null, "label", "structSize"), problems.map { it.field })
    assertTrue("kotlin.String" in problems[1].message)
    assertEquals(1, GpuStructRules.problems(empty).size)
  }

  @Test
  fun `a nested struct's problems are reported on the field holding it`() {
    val inner =
      GpuStructModel(
        "test",
        "Inner",
        listOf(GpuFieldModel("x", GpuFieldType.Unsupported("kotlin.Long"))),
      )
    val outer =
      GpuStructModel("test", "Outer", listOf(GpuFieldModel("inner", GpuFieldType.Struct(inner))))

    val problems = GpuStructRules.problems(outer)

    assertEquals(listOf("inner"), problems.map { it.field })
    assertTrue("nested Inner" in problems.single().message)
  }

  @Test
  fun `WGSL keywords and reserved words name neither a struct nor a field`() {
    val model =
      GpuStructModel(
        "test",
        "Self",
        listOf("target", "loop", "fine").map { GpuFieldModel(it, f32) },
      )

    val problems = GpuStructRules.problems(model)

    assertEquals(listOf(null, "target", "loop"), problems.map { it.field })
  }
}

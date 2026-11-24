package dev.chieppa.ease.codegen

import dev.chieppa.ease.codegen.model.ComponentModel
import dev.chieppa.ease.codegen.model.FieldAudience
import dev.chieppa.ease.codegen.model.FieldModel
import dev.chieppa.ease.codegen.model.Quantization
import dev.chieppa.ease.codegen.rules.ModelRules
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Which models the processor accepts, on models built in code. */
class ModelRulesTest {
  private fun model(vararg fields: FieldModel) =
    ComponentModel("test", "Lamp", "test.Lamp", fields.toList())

  private fun float(name: String, quantization: Quantization? = null) =
    FieldModel(name, FieldModel.FLOAT, quantization = quantization)

  private fun problemFields(model: ComponentModel): List<String?> =
    ModelRules.problems(model).map { it.field }

  @Test
  fun `floats, ints and booleans with valid options pass`() {
    val model =
      model(
        float("level", Quantization(8, 0f, 1f)),
        FieldModel("count", FieldModel.INT, audience = FieldAudience.OWNER_ONLY),
        FieldModel("on", FieldModel.BOOLEAN, interpolated = true),
      )

    assertEquals(emptyList(), ModelRules.problems(model))
  }

  @Test
  fun `unsupported and nullable types are refused by field`() {
    val model =
      model(
        FieldModel("label", "kotlin.String"),
        FieldModel("maybe", FieldModel.FLOAT, nullable = true),
      )

    assertEquals(listOf<String?>("label", "maybe"), problemFields(model))
  }

  @Test
  fun `quantization needs a float, 1 to 30 bits and a finite range`() {
    val model =
      model(
        FieldModel("count", FieldModel.INT, quantization = Quantization(8, 0f, 1f)),
        float("wide", Quantization(31, 0f, 1f)),
        float("empty", Quantization(8, 1f, 1f)),
        float("endless", Quantization(8, 0f, Float.POSITIVE_INFINITY)),
      )

    assertEquals(listOf<String?>("count", "wide", "empty", "endless"), problemFields(model))
  }

  @Test
  fun `a class must be a component with a type companion and a constructor without arguments`() {
    val model =
      model(float("level"))
        .copy(isComponent = false, hasTypeCompanion = false, hasNoArgumentConstructor = false)

    val problems = ModelRules.problems(model)

    assertEquals(3, problems.size)
    assertTrue(problems.all { it.field == null })
  }

  @Test
  fun `a class has at most 32 fields`() {
    val fields = List(ModelRules.MAX_FIELDS + 1) { float("f$it") }

    assertEquals(listOf<String?>(null), problemFields(model(*fields.toTypedArray())))
  }
}

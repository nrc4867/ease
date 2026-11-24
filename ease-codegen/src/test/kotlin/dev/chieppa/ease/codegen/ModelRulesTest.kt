package dev.chieppa.ease.codegen

import dev.chieppa.ease.codegen.model.ClassVisibility
import dev.chieppa.ease.codegen.model.ComponentModel
import dev.chieppa.ease.codegen.model.FieldAudience
import dev.chieppa.ease.codegen.model.FieldModel
import dev.chieppa.ease.codegen.model.ValueRange
import dev.chieppa.ease.codegen.rules.ModelRules
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Which models the processor accepts, on models built in code. */
class ModelRulesTest {
  private fun model(vararg fields: FieldModel) =
    ComponentModel("test", "Lamp", "test.Lamp", fields.toList())

  private fun float(name: String, bits: Int? = null, range: ValueRange? = null) =
    FieldModel(name, FieldModel.FLOAT, quantizedBits = bits, range = range)

  private val unit = ValueRange(0.0, 1.0)

  private fun problemFields(model: ComponentModel): List<String?> =
    ModelRules.problems(model).map { it.field }

  @Test
  fun `floats, ints and booleans with valid options pass`() {
    val model =
      model(
        float("level", bits = 8, range = unit),
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
        FieldModel("count", FieldModel.INT, quantizedBits = 8, range = unit),
        float("wide", bits = 31, range = unit),
        float("empty", bits = 8, range = ValueRange(1.0, 1.0)),
        float("endless", bits = 8, range = ValueRange(0.0, Double.POSITIVE_INFINITY)),
      )

    assertEquals(listOf<String?>("count", "wide", "empty", "endless"), problemFields(model))
  }

  @Test
  fun `a quantized field without a range is refused, whatever its bits`() {
    val model = model(float("level", bits = 8), float("plain", range = unit))

    val problems = ModelRules.problems(model)

    assertEquals(listOf<String?>("level"), problems.map { it.field })
    assertTrue("@Range" in problems.single().message, problems.single().message)
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
  fun `a field whose setter is private or protected is refused`() {
    val model = model(FieldModel("level", FieldModel.FLOAT, settable = false), float("open"))

    assertEquals(listOf<String?>("level"), problemFields(model))
  }

  @Test
  fun `a class without a constructor generated code can call is refused`() {
    val model = model(float("level")).copy(hasNoArgumentConstructor = false)

    val problems = ModelRules.problems(model)

    assertEquals(1, problems.size)
    assertTrue("constructor" in problems.single().message)
  }

  @Test
  fun `a class hidden by its own or an outer class's visibility is refused`() {
    val hidden = model(float("level")).copy(visibility = ClassVisibility.HIDDEN)
    val internal = model(float("level")).copy(visibility = ClassVisibility.INTERNAL)

    assertEquals(listOf<String?>(null), problemFields(hidden))
    assertEquals(emptyList(), ModelRules.problems(internal))
  }

  @Test
  fun `a class has at most 32 fields`() {
    val fields = List(ModelRules.MAX_FIELDS + 1) { float("f$it") }

    assertEquals(listOf<String?>(null), problemFields(model(*fields.toTypedArray())))
  }
}

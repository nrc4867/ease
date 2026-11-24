package dev.chieppa.ease.codegen

import dev.chieppa.ease.codegen.model.PrimitiveKind
import dev.chieppa.ease.codegen.model.SerialField
import dev.chieppa.ease.codegen.model.SerialType
import dev.chieppa.ease.codegen.model.TypeKind
import dev.chieppa.ease.codegen.model.TypeRef
import dev.chieppa.ease.codegen.model.ValueRange
import dev.chieppa.ease.codegen.rules.SchemaRules
import kotlin.test.Test
import kotlin.test.assertEquals

/** Which `@Range`s the schema outputs accept, on types built in code. */
class SchemaRulesTest {
  private val number = TypeRef.Primitive(PrimitiveKind.NUMBER)

  private fun type(vararg fields: SerialField) =
    SerialType("test.Lamp", TypeKind.CLASS, fields = fields.toList())

  private fun problemFields(type: SerialType) = SchemaRules.problems(type).map { it.field }

  @Test
  fun `a range on a number, nullable or not, passes`() {
    val range = ValueRange(0.0, 1.0)
    val type =
      type(
        SerialField("level", number, range = range),
        SerialField("maybe", TypeRef.Nullable(number), range = range),
        SerialField("plain", number),
      )

    assertEquals(emptyList(), SchemaRules.problems(type))
  }

  @Test
  fun `a range on a string, a list or a named type is refused`() {
    val range = ValueRange(0.0, 1.0)
    val type =
      type(
        SerialField("label", TypeRef.Primitive(PrimitiveKind.STRING), range = range),
        SerialField("steps", TypeRef.ListOf(number), range = range),
        SerialField("mood", TypeRef.Named("test.Mood"), range = range),
      )

    assertEquals(listOf<String?>("label", "steps", "mood"), problemFields(type))
  }

  @Test
  fun `a range needs finite ends with min below max`() {
    val type =
      type(
        SerialField("empty", number, range = ValueRange(1.0, 1.0)),
        SerialField("endless", number, range = ValueRange(0.0, Double.POSITIVE_INFINITY)),
        SerialField("unset", number, range = ValueRange(Double.NaN, 1.0)),
      )

    assertEquals(listOf<String?>("empty", "endless", "unset"), problemFields(type))
  }
}

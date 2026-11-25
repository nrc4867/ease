package dev.chieppa.ease.codegen

import dev.chieppa.ease.codegen.model.ComponentModel
import dev.chieppa.ease.codegen.model.FieldKind
import dev.chieppa.ease.codegen.model.FieldModel
import dev.chieppa.ease.codegen.output.SerializerSource
import dev.chieppa.ease.codegen.rules.ModelRules
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** An enum field replicates as its ordinal. */
class EnumFieldTest {
  private val shape = FieldModel("shape", "test.Shape", isEnum = true)
  private val model =
    ComponentModel(
      packageName = "test",
      className = "Body",
      serialName = "test.Body",
      fields = listOf(shape),
    )

  @Test
  fun `an enum field goes on the wire as its ordinal and reads back through its entries`() {
    val source = SerializerSource.of(model)

    assertEquals(FieldKind.ENUM, shape.kind)
    assertTrue("ReplicatedFields.writeInt(writer, value.shape.ordinal)" in source, source)
    assertTrue("test.Shape.entries[ReplicatedFields.readInt(reader)]" in source, source)
  }

  @Test
  fun `an enum field breaks no rule, and a class that is not an enum still does`() {
    val other = FieldModel("label", "kotlin.String")

    assertEquals(emptyList(), ModelRules.problems(model).filter { it.field == "shape" })
    assertEquals(
      listOf("label"),
      ModelRules.problems(model.copy(fields = listOf(other))).mapNotNull { it.field },
    )
  }
}

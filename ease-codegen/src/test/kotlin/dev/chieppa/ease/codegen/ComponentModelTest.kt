package dev.chieppa.ease.codegen

import dev.chieppa.ease.codegen.model.ComponentModel
import dev.chieppa.ease.codegen.model.FieldAudience
import dev.chieppa.ease.codegen.model.FieldModel
import dev.chieppa.ease.codegen.output.ComponentMasks
import kotlin.test.Test
import kotlin.test.assertEquals

/** The masks and names a model gives its serializer. */
class ComponentModelTest {
  private val model =
    ComponentModel(
      packageName = "test",
      className = "Outer.Lamp",
      serialName = "test.Lamp",
      fields =
        listOf(
          FieldModel("level", FieldModel.FLOAT, interpolated = true),
          FieldModel("count", FieldModel.INT, audience = FieldAudience.OWNER_ONLY),
          FieldModel("on", FieldModel.BOOLEAN, audience = FieldAudience.ALL_BUT_OWNER),
        ),
    )

  @Test
  fun `masks follow each field's audience and interpolation, the first field lowest`() {
    assertEquals(0b001, model.interpolatedFields)
    assertEquals(0b011, model.ownerFields)
    assertEquals(0b101, model.othersFields)
  }

  @Test
  fun `the serializer is named after the class itself, nested or not`() {
    assertEquals("LampReplicated", model.serializerName)
    assertEquals("test.Outer.Lamp", model.qualifiedName)
  }

  @Test
  fun `a mask using the top bit is spelled as an Int`() {
    assertEquals("5", ComponentMasks.literal(0b101))
    assertEquals("(Int.MIN_VALUE or 1)", ComponentMasks.literal(Int.MIN_VALUE or 1))
    assertEquals(-1, ComponentMasks.all(ComponentMasks.INT_BITS))
    assertEquals(0b111, ComponentMasks.all(3))
  }
}

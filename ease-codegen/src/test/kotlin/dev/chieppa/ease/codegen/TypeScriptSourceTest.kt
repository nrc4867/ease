package dev.chieppa.ease.codegen

import dev.chieppa.ease.codegen.model.DescribedType
import dev.chieppa.ease.codegen.model.PrimitiveKind
import dev.chieppa.ease.codegen.model.SerialField
import dev.chieppa.ease.codegen.model.SerialType
import dev.chieppa.ease.codegen.model.TypeKind
import dev.chieppa.ease.codegen.model.TypeRef
import dev.chieppa.ease.codegen.model.ValueRange
import dev.chieppa.ease.codegen.output.ResourceFiles
import dev.chieppa.ease.codegen.output.TypeScriptSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The TypeScript a module's types become, on types built in code. */
class TypeScriptSourceTest {
  private val number = TypeRef.Primitive(PrimitiveKind.NUMBER)
  private val text = TypeRef.Primitive(PrimitiveKind.STRING)

  private fun described(vararg types: SerialType) = types.map {
    DescribedType(it, types.associateBy { type -> type.serialName })
  }

  private val marker = Regex("// ease-(declared|reached): ")

  /** The block of [serialName], from the marker line to the next block. */
  private fun block(source: String, serialName: String): String =
    source.split(marker).single { it.lineSequence().first() == serialName }

  private fun classOf(name: String, vararg fields: SerialField, discriminated: Boolean = false) =
    SerialType(name, TypeKind.CLASS, fields = fields.toList(), discriminated = discriminated)

  private fun valueOf(name: String) =
    SerialType(name, TypeKind.VALUE, fields = listOf(SerialField("value", number)))

  @Test
  fun `a class is an interface in the namespace of its serial name, with docs and range tags`() {
    val lamp =
      SerialType(
        "game.Lamp",
        TypeKind.CLASS,
        doc = "A lamp.",
        fields =
          listOf(
            SerialField("level", number, doc = "How bright.", range = ValueRange(0.0, 1.5)),
            SerialField("label", TypeRef.Nullable(text), optional = true),
          ),
      )

    val block = block(TypeScriptSource.of(described(lamp)), "game.Lamp")

    assertTrue("declare namespace game {" in block, block)
    assertTrue("/** A lamp. */" in block, block)
    assertTrue("interface Lamp {" in block, block)
    assertTrue("@minimum 0" in block && "@maximum 1.5" in block, block)
    assertTrue("level: number;" in block, block)
    assertTrue("label?: string | null;" in block, block)
  }

  @Test
  fun `a member of a polymorphic base has a typed form, and the plain interface has no type`() {
    val destroy = classOf("ease.Destroy", discriminated = true)

    val block = block(TypeScriptSource.of(described(destroy)), "ease.Destroy")

    assertTrue("interface Destroy {" in block, block)
    assertTrue("interface DestroyTyped extends Destroy {" in block, block)
    assertTrue("type: \"ease.Destroy\";" in block, block)
    assertEquals(1, Regex("type: ").findAll(block).count(), block)
  }

  @Test
  fun `a class outside a polymorphic base has no typed form`() {
    val block = block(TypeScriptSource.of(described(classOf("game.Plain"))), "game.Plain")

    assertTrue("Typed" !in block, block)
  }

  @Test
  fun `enums, value classes and sealed hierarchies are type aliases of the typed forms`() {
    val mood = SerialType("game.Mood", TypeKind.ENUM, values = listOf("CALM", "angry"))
    val weather =
      SerialType("game.Weather", TypeKind.SEALED, subtypes = listOf("game.Rain", "game.Sun"))
    val rain = classOf("game.Rain", discriminated = true)
    val sun = classOf("game.Sun", discriminated = true)

    val source = TypeScriptSource.of(described(mood, valueOf("game.Id"), weather, rain, sun))

    assertTrue("type Mood = \"CALM\" | \"angry\";" in block(source, "game.Mood"))
    assertTrue("type Id = number;" in block(source, "game.Id"))
    assertTrue("type Weather = game.RainTyped | game.SunTyped;" in block(source, "game.Weather"))
  }

  @Test
  fun `a sealed hierarchy with no serializable subtype is never`() {
    val none = SerialType("game.Empty", TypeKind.SEALED)

    val block = block(TypeScriptSource.of(described(none)), "game.Empty")

    assertTrue("type Empty = never;" in block, block)
  }

  @Test
  fun `a property name that is not an identifier is quoted`() {
    val fast = classOf("game.Fast", SerialField("max-speed", number), SerialField("speed", number))

    val block = block(TypeScriptSource.of(described(fast)), "game.Fast")

    assertTrue("\"max-speed\": number;" in block, block)
    assertTrue("  speed: number;" in block, block)
  }

  @Test
  fun `a range end beyond what a Long holds is spelled as the double it is`() {
    val big = classOf("game.Big", SerialField("size", number, range = ValueRange(0.0, 1e20)))

    val block = block(TypeScriptSource.of(described(big)), "game.Big")

    assertTrue("@maximum 1.0E20" in block, block)
    assertTrue("9223372036854775807" !in block, block)
  }

  @Test
  fun `lists, maps, named and open types are spelled out, and open ones bring their base`() {
    val crate =
      classOf(
        "game.Crate",
        SerialField("ids", TypeRef.ListOf(TypeRef.Named("game.Id"))),
        SerialField("sizes", TypeRef.MapOf(text, number)),
        SerialField("parts", TypeRef.ListOf(TypeRef.Open("Component"))),
        SerialField("maybes", TypeRef.ListOf(TypeRef.Nullable(number))),
      )

    val source = TypeScriptSource.of(described(crate))

    val block = block(source, "game.Crate")
    assertTrue("ids: game.Id[];" in block, block)
    assertTrue("sizes: Record<string, number>;" in block, block)
    assertTrue("parts: ease.Typed[];" in block, block)
    assertTrue("maybes: (number | null)[];" in block, block)
    assertTrue("interface Typed {" in block(source, "ease.Typed"))
  }

  @Test
  fun `each type appears once however many types reach it`() {
    val id = TypeRef.Named("game.Id")
    val a = classOf("game.A", SerialField("id", id))
    val b = classOf("game.B", SerialField("id", id))

    val source = TypeScriptSource.of(described(a, b, valueOf("game.Id")))

    assertEquals(1, Regex("ease-(declared|reached): game.Id").findAll(source).count())
  }

  @Test
  fun `a block says whether the module declares the type or only reaches it`() {
    val id = valueOf("game.Id")
    val lamp = classOf("game.Lamp", SerialField("id", TypeRef.Named("game.Id")))
    val reachable = mapOf("game.Lamp" to lamp, "game.Id" to id)

    val source = TypeScriptSource.of(listOf(DescribedType(lamp, reachable)))

    assertTrue("${ResourceFiles.DECLARED_MARKER}game.Lamp" in source, source)
    assertTrue("${ResourceFiles.REACHED_MARKER}game.Id" in source, source)
  }
}

package dev.chieppa.ease.architecture

import com.lemonappdev.konsist.api.Konsist
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Every public serialized type and field in the engine and the games carries `@Doc`. */
class DocRuleTest {
  @Test
  fun `every public serialized type and field in main sources has a Doc`() {
    val violations = DocRule.violations(Konsist.scopeFromProduction())

    assertEquals(emptyList(), violations)
  }

  private fun violationsIn(code: String): List<String> {
    val directory = Files.createTempDirectory("doc-rule")
    Files.writeString(directory.resolve("Snippet.kt"), code)
    return DocRule.violations(Konsist.scopeFromExternalDirectory(directory.toString()))
  }

  @Test
  fun `a public serializable class without a Doc is a violation`() {
    val violations = violationsIn("@Serializable class Lamp(@Doc(\"x\") val level: Float)")

    assertEquals(1, violations.size)
    assertTrue(violations.single().endsWith(": Lamp"), violations.single())
  }

  @Test
  fun `a public field of a documented serializable class needs a Doc too`() {
    val code =
      """
      @Doc("a lamp")
      @Serializable
      class Lamp(@Doc("how bright") val level: Float, val color: Int) {
        var on: Boolean = false
      }
      """
        .trimIndent()

    val violations = violationsIn(code)

    assertEquals(listOf("Lamp.color", "Lamp.on"), violations.map { it.substringAfter(": ") })
  }

  @Test
  fun `internal types, private fields, transient and computed ones need no Doc`() {
    val code =
      """
      @Serializable
      internal class Hidden(val level: Float)

      @Doc("a lamp")
      @Serializable
      class Lamp(@Doc("how bright") val level: Float) {
        private var cache: Int = 0
        @Transient var scratch: Int = 0
        val isBright: Boolean get() = level > 1f
        companion object { const val MAX = 1 }
      }

      class Plain(val level: Float)
      """
        .trimIndent()

    assertEquals(emptyList(), violationsIn(code))
  }
}

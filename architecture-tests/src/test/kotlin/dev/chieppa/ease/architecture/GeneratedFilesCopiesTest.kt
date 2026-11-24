package dev.chieppa.ease.architecture

import com.lemonappdev.konsist.api.Konsist
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The names of the files `ease-codegen` writes and the markers of its TypeScript parts are copied
 * into the build tool and the build logic, which cannot depend on `ease-annotations`; the copies
 * must equal the original.
 */
class GeneratedFilesCopiesTest {
  private val files = Konsist.scopeFromProject().files

  /** The `const val` strings of the file whose path ends with [suffix], by name. */
  private fun constants(suffix: String): Map<String, String> {
    val file = files.single { it.path.replace("\\", "/").endsWith(suffix) }
    return CONSTANT.findAll(file.text).associate { it.groupValues[1] to it.groupValues[2] }
  }

  private val original =
    constants("ease-annotations/src/main/kotlin/dev/chieppa/ease/annotations/GeneratedFiles.kt")

  private fun differences(copy: Map<String, String>, names: List<String>): List<String> =
    names.filter {
      copy[it] != original[it]
    }

  @Test
  fun `the original holds every name`() {
    assertEquals(
      setOf("DIRECTORY", "SCHEMAS_FILE", "TYPES_FILE", "DECLARED_MARKER", "REACHED_MARKER"),
      original.keys,
    )
  }

  @Test
  fun `the build tool's copy equals the original`() {
    val copy =
      constants("ease-codegen/src/main/kotlin/dev/chieppa/ease/codegen/output/ResourceFiles.kt")

    assertEquals(emptyList(), differences(copy, original.keys.toList()))
  }

  @Test
  fun `the build logic's copy equals the original`() {
    val copy =
      constants("build-logic/src/main/kotlin/dev/chieppa/ease/build/types/TypeFragments.kt")

    assertEquals(
      emptyList(),
      differences(copy, listOf("DIRECTORY", "TYPES_FILE", "DECLARED_MARKER", "REACHED_MARKER")),
    )
  }

  private companion object {
    val CONSTANT = Regex("const val (\\w+)(?::\\s*String)?\\s*=\\s*\"([^\"]*)\"")
  }
}

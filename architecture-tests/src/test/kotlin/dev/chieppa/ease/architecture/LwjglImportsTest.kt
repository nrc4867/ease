package dev.chieppa.ease.architecture

import com.lemonappdev.konsist.api.Konsist
import kotlin.test.Test
import kotlin.test.assertEquals

/** LWJGL stays behind ease-platform's interfaces: no other module imports it. */
class LwjglImportsTest {
  @Test
  fun `only ease-platform imports LWJGL`() {
    val violations =
      Konsist.scopeFromProject()
        .files
        .filter { it.moduleName != "ease-platform" }
        .flatMap { file ->
          file.imports
            .filter { it.name.startsWith("org.lwjgl.") }
            .map { "${file.path}: ${it.name}" }
        }

    assertEquals(emptyList(), violations)
  }
}

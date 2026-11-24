package dev.chieppa.ease.build.sources

import kotlin.test.Test
import kotlin.test.assertEquals

class SourceFileTest {
  @Test
  fun `test source sets are told from main ones`() {
    val paths =
      listOf(
        "ease-ecs/src/test/kotlin/A.kt",
        "ease-gpu/src/gpuTest/kotlin/A.kt",
        "ease-ecs/src/main/kotlin/A.kt",
        "ease-ecs/src/main/kotlin/test/A.kt",
      )

    val tests = paths.map { SourceFile(it, emptyList()).isTest }

    assertEquals(listOf(true, true, false, false), tests)
  }
}

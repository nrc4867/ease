package dev.chieppa.ease.build.sources

import kotlin.test.Test
import kotlin.test.assertEquals

class GoldenFileRuleTest {
  private val rule = GoldenFileRule()

  @Test
  fun `expected-output files under tests fail`() {
    val paths =
      listOf(
        "ease-render/src/test/resources/goldens/frame.png",
        "ease-ecs/src/test/resources/world.golden.json",
        "ease-ecs/src/test/resources/expected/world.json",
        "ease-ecs/src/test/resources/world.expected.json",
      )

    val failing = paths.filter { rule.check(SourceFile(it, emptyList())).isNotEmpty() }

    assertEquals(paths, failing)
  }

  @Test
  fun `a flag that rewrites goldens fails`() {
    val file = SourceFile("build.gradle.kts", listOf("if (hasProperty(\"updateGoldens\")) {"))

    assertEquals(listOf(1), rule.check(file).map { it.line })
  }

  @Test
  fun `ordinary test resources pass`() {
    val file = SourceFile("ease-ecs/src/test/resources/world.json", emptyList())

    assertEquals(emptyList(), rule.check(file))
  }
}

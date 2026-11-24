package dev.chieppa.ease.build.layers

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ModuleLayersTest {
  @Test
  fun `a module may depend on a lower layer`() {
    assertNull(layerViolation("ease-render", "ease-gpu"))
  }

  @Test
  fun `a module may not depend on its own layer or above`() {
    assertNotNull(layerViolation("ease-gpu", "ease-render"))
    assertNotNull(layerViolation("ease-runtime", "sample"))
  }

  @Test
  fun `a module's package follows its name`() {
    assertEquals("dev.chieppa.ease.math", modulePackage("ease-math"))
    assertEquals("dev.chieppa.ease.sample", modulePackage("sample"))
  }

  @Test
  fun `a test-support module may be taken only by test configurations`() {
    assertNull(testSupportViolation("ease-math", "ease-testing", "testImplementation"))
    assertNotNull(testSupportViolation("ease-math", "ease-testing", "implementation"))
    assertNotNull(testSupportViolation("ease-ecs", "ease-testing", "api"))
  }

  @Test
  fun `only a test-support module is subject to the test-only rule`() {
    assertNull(testSupportViolation("ease-ecs", "ease-math", "api"))
  }
}

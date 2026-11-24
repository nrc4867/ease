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
  fun `the annotations module sits below the core, so every module with serialized types can use it`() {
    assertNull(layerViolation("ease-ecs", "ease-annotations"))
    assertNull(layerViolation("sample", "ease-annotations"))
    assertNotNull(layerViolation("ease-annotations", "ease-ecs"))
  }

  @Test
  fun `assets sit above the platform and below the gpu`() {
    assertNull(layerViolation("ease-assets", "ease-platform"))
    assertNull(layerViolation("ease-assets", "ease-commands"))
    assertNull(layerViolation("ease-gpu", "ease-assets"))
    assertNotNull(layerViolation("ease-platform", "ease-assets"))
  }

  @Test
  fun `the serialization module sits between the core and the world`() {
    assertNull(layerViolation("ease-serialization", "ease-ecs"))
    assertNull(layerViolation("ease-commands", "ease-serialization"))
    assertNotNull(layerViolation("ease-ecs", "ease-serialization"))
  }

  @Test
  fun `a module may not depend on its own layer or above`() {
    assertNotNull(layerViolation("ease-gpu", "ease-render"))
    assertNotNull(layerViolation("ease-runtime", "sample"))
  }

  @Test
  fun `a module may take its own parts, such as its test fixtures`() {
    assertNull(layerViolation("ease-agent", "ease-agent"))
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

  @Test
  fun `a build-tool module may be taken only by ksp configurations`() {
    assertNull(buildToolViolation("ease-ecs", "ease-codegen", "ksp"))
    assertNull(buildToolViolation("sample", "ease-codegen", "kspTest"))
    assertNotNull(buildToolViolation("ease-ecs", "ease-codegen", "implementation"))
    assertNotNull(buildToolViolation("ease-ecs", "ease-codegen", "testImplementation"))
    assertNotNull(buildToolViolation("ease-ecs", "ease-codegen", "compileOnly"))
  }

  @Test
  fun `only a build-tool module is subject to the ksp-only rule`() {
    assertNull(buildToolViolation("ease-ecs", "ease-math", "api"))
  }

  @Test
  fun `a build-tool module ranks below every module, so it can use none of them`() {
    assertNotNull(layerViolation("ease-codegen", "ease-testing"))
    assertNotNull(layerViolation("ease-codegen", "ease-ecs"))
  }
}

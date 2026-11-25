package dev.chieppa.ease.memory

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** Struct declarations joined so each comes once, nested structs first. */
class WgslDeclarationsTest {
  private class Layout(override val wgslName: String, override val nested: List<GpuLayout>) :
    GpuLayout {
    override val wgsl = "struct $wgslName {}"
    override val byteSize = 16L
    override val alignment = 16L
  }

  @Test
  fun `nested structs come before their holders and each struct once`() {
    val light = Layout("Light", emptyList())
    val frame = Layout("Frame", listOf(light))
    val view = Layout("View", listOf(light))

    val text = wgslDeclarations(listOf(frame, view, light))

    assertEquals("struct Light {}\nstruct Frame {}\nstruct View {}", text)
  }

  @Test
  fun `two different structs with one name are refused`() {
    val frame = Layout("Frame", listOf(Layout("Light", emptyList())))
    val other = Layout("Light", emptyList())

    val error = assertFailsWith<IllegalArgumentException> { wgslDeclarations(listOf(frame, other)) }

    assertTrue("WGSL struct Light" in error.message.orEmpty(), error.message)
  }
}

package dev.chieppa.ease.gpu.lit

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The lit pass's structs as ease-codegen lays them out under WGSL's rules. */
class GpuStructLayoutsTest {
  @Test
  fun `a scalar after a vec3 takes its fourth slot and the next vec3 starts on 16`() {
    assertEquals(0L, DirectionalLightDataWriter.DIRECTION)
    assertEquals(12L, DirectionalLightDataWriter.INTENSITY)
    assertEquals(16L, DirectionalLightDataWriter.COLOR)
    assertEquals(32L, DirectionalLightDataWriter.STRUCT_SIZE)
    assertEquals(listOf(0L, 12L, 16L, 28L, 32L), materialLayout())
  }

  @Test
  fun `a nested struct is followed on a 16 byte boundary and declared before its holder`() {
    assertEquals(0L, FrameDataWriter.LIGHT)
    assertEquals(32L, FrameDataWriter.AMBIENT)
    assertEquals(48L, FrameDataWriter.STRUCT_SIZE)
    assertEquals(listOf(DirectionalLightDataWriter), FrameDataWriter.nested)
    val shader = forwardShader(MaterialTemplate.LIT)
    assertTrue(shader.indexOf("struct DirectionalLightData") < shader.indexOf("struct FrameData"))
  }

  @Test
  fun `matrices take 64 bytes on a 16 byte alignment`() {
    assertEquals(64L, ViewDataWriter.CAMERA_POSITION)
    assertEquals(80L, ViewDataWriter.STRUCT_SIZE)
    assertEquals(64L, ObjectDataWriter.NORMAL_MATRIX)
    assertEquals(128L, ObjectDataWriter.STRUCT_SIZE)
  }

  private fun materialLayout() =
    listOf(
      LitMaterialWriter.BASE_COLOR,
      LitMaterialWriter.ROUGHNESS,
      LitMaterialWriter.EMISSIVE,
      LitMaterialWriter.METALLIC,
      LitMaterialWriter.STRUCT_SIZE,
    )
}

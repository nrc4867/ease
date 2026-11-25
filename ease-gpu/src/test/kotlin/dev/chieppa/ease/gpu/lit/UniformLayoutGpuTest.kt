package dev.chieppa.ease.gpu.lit

import dev.chieppa.ease.gpu.GpuContext
import dev.chieppa.ease.gpu.ffi.createShaderModule
import dev.chieppa.ease.memory.GpuStruct
import dev.chieppa.ease.memory.wgslDeclarations
import dev.chieppa.webgpu.ffi.wgpu_h
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Two floats: 8 bytes aligned to 8, too small for a uniform struct member without attributes. */
@GpuStruct internal class Pair2(val first: Float, val second: Float)

/** A struct nesting [Pair2] between scalars, which WGSL's uniform rules pad. */
@GpuStruct internal class Padded(val flag: Float, val pair: Pair2, val after: Float)

/**
 * Generated WGSL compiled by the GPU's shader compiler, which rejects a uniform struct whose layout
 * breaks the address space's rules: the attributes codegen adds make it valid, and the offsets
 * Kotlin writes at are the ones those attributes give.
 */
class UniformLayoutGpuTest {
  @Test
  fun `a nested struct in a uniform compiles with the offsets the writer uses`() {
    val shader =
      wgslDeclarations(listOf(PaddedWriter)) +
        """
        @group(0) @binding(0) var<uniform> padded: Padded;
        @fragment fn fragment_main() -> @location(0) vec4f {
          return vec4f(padded.flag, padded.pair.first, padded.after, 1.0);
        }
        """

    GpuContext.open().use { gpu ->
      val module = createShaderModule(gpu.device, shader, "padded")
      gpu.checkErrors()
      wgpu_h.wgpuShaderModuleRelease(module)
    }

    assertTrue("@align(16) @size(16) pair: Pair2" in PaddedWriter.STRUCT_WGSL)
    assertEquals(
      listOf(0L, 16L, 32L),
      listOf(PaddedWriter.FLAG, PaddedWriter.PAIR, PaddedWriter.AFTER),
    )
  }

  @Test
  fun `the lit forward shader with its generated structs compiles`() {
    GpuContext.open().use { gpu ->
      val module = createShaderModule(gpu.device, forwardShader(MaterialTemplate.LIT), "lit")
      gpu.checkErrors()
      wgpu_h.wgpuShaderModuleRelease(module)
    }
  }
}

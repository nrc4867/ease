package dev.chieppa.ease.gpu.compute

import dev.chieppa.ease.gpu.CommandEncoder
import dev.chieppa.ease.gpu.GpuContext
import java.lang.foreign.Arena
import java.lang.foreign.ValueLayout
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.Test
import kotlin.test.assertContentEquals

/** A kernel reads one buffer and writes another, and the result reads back. */
class ComputeKernelGpuTest {
  @Test
  fun `a kernel doubles numbers written into a buffer`() {
    val input = IntArray(COUNT) { it * 3 }
    val output = GpuContext.open().use { gpu -> doubled(gpu, input) }

    val values = ByteBuffer.wrap(output).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer()
    assertContentEquals(input.map { it * 2 }, List(COUNT) { values.get(it) })
  }

  private fun doubled(gpu: GpuContext, input: IntArray): ByteArray {
    val source = GpuBuffer(gpu, COUNT * 4L, BufferUse.STORAGE)
    val target = GpuBuffer(gpu, COUNT * 4L, BufferUse.STORAGE)
    val kernel = ComputeKernel(gpu, DOUBLE, "main")
    val bindings = kernel.bind(0, listOf(source, target))
    try {
      Arena.ofConfined().use { arena ->
        source.write(arena.allocateFrom(ValueLayout.JAVA_INT, *input))
      }
      val encoder = CommandEncoder.on(gpu)
      kernel.dispatch(encoder, listOf(bindings), (COUNT + 63) / 64)
      encoder.submit()
      return target.read()
    } finally {
      listOf(bindings, kernel, target, source).forEach(AutoCloseable::close)
    }
  }

  private companion object {
    const val COUNT = 100
    val DOUBLE =
      """
      @group(0) @binding(0) var<storage, read> source: array<i32>;
      @group(0) @binding(1) var<storage, read_write> doubled: array<i32>;

      @compute @workgroup_size(64)
      fn main(@builtin(global_invocation_id) id: vec3u) {
        if (id.x < arrayLength(&source)) {
          doubled[id.x] = source[id.x] * 2;
        }
      }
      """
        .trimIndent()
  }
}

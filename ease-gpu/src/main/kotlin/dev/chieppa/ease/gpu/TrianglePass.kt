package dev.chieppa.ease.gpu

import dev.chieppa.ease.gpu.ffi.BufferBinding
import dev.chieppa.ease.gpu.ffi.Flags
import dev.chieppa.ease.gpu.ffi.FrameCalls
import dev.chieppa.ease.gpu.ffi.beginColorPass
import dev.chieppa.ease.gpu.ffi.createBuffer
import dev.chieppa.ease.gpu.ffi.createBufferBindGroup
import dev.chieppa.ease.gpu.ffi.createColorPipeline
import dev.chieppa.ease.gpu.ffi.createShaderModule
import dev.chieppa.webgpu.ffi.wgpu_h
import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

/** The shader's uniform block: angle, aspect ratio and padding to 16 bytes. */
private const val PARAMS_BYTES = 16L

private const val ANGLE_OFFSET = 0L
private const val ASPECT_OFFSET = 4L
private const val VERTICES = 3

/**
 * Clears a target to [clear] and draws one colored triangle on it, turned by an angle. The first
 * thing Ease draws, so a window, an offscreen texture and an editor viewport can all be checked
 * against the same picture.
 */
public class TrianglePass(private val context: GpuContext, public val clear: Color = CLEAR) :
  AutoCloseable {
  private val shader =
    createShaderModule(context.device, loadShader(), "triangle").also { context.checkErrors() }
  private val params =
    createBuffer(context.device, PARAMS_BYTES, Flags.BUFFER_UNIFORM or Flags.BUFFER_COPY_DST)
  private val pipelines = mutableMapOf<TextureFormat, Pair<MemorySegment, MemorySegment>>()
  private val staging = Arena.ofConfined().let { it to it.allocate(PARAMS_BYTES) }

  /**
   * Records the pass into [encoder]: [target] cleared and the triangle drawn turned by [angle]
   * radians, undistorted at the target's aspect ratio.
   */
  public fun record(encoder: CommandEncoder, target: RenderTarget, angle: Float = 0f) {
    val (pipeline, bindGroup) = pipelines.getOrPut(target.format) { pipelineFor(target.format) }
    val bytes = staging.second
    bytes.set(ValueLayout.JAVA_FLOAT, ANGLE_OFFSET, angle)
    bytes.set(
      ValueLayout.JAVA_FLOAT,
      ASPECT_OFFSET,
      target.size.width.toFloat() / target.size.height,
    )
    wgpu_h.wgpuQueueWriteBuffer(context.queue, params, 0, bytes, PARAMS_BYTES)
    val pass = beginColorPass(context.frames, encoder.address, target.view, clear)
    FrameCalls.setPipeline(pass, pipeline)
    FrameCalls.setBindGroup(pass, 0, bindGroup)
    FrameCalls.draw(pass, VERTICES, 1)
    FrameCalls.endPass(pass)
  }

  override fun close() {
    pipelines.values.forEach { (pipeline, bindGroup) ->
      wgpu_h.wgpuBindGroupRelease(bindGroup)
      wgpu_h.wgpuRenderPipelineRelease(pipeline)
    }
    wgpu_h.wgpuBufferRelease(params)
    wgpu_h.wgpuShaderModuleRelease(shader)
    staging.first.close()
  }

  private fun pipelineFor(format: TextureFormat): Pair<MemorySegment, MemorySegment> {
    val pipeline = createColorPipeline(context.device, shader, format.native, "triangle")
    val bindGroup =
      createBufferBindGroup(context.device, pipeline, listOf(BufferBinding(params, PARAMS_BYTES)))
    context.checkErrors()
    return pipeline to bindGroup
  }

  public companion object {
    /** The color the pass clears to: a dark slate no triangle corner comes near. */
    public val CLEAR: Color = Color(0.08f, 0.09f, 0.12f)

    private fun loadShader(): String =
      checkNotNull(TrianglePass::class.java.getResource("triangle.wgsl")) { "triangle.wgsl" }
        .readText()
  }
}

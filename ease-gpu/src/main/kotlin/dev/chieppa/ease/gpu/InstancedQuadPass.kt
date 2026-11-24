package dev.chieppa.ease.gpu

import dev.chieppa.ease.gpu.ffi.BufferBinding
import dev.chieppa.ease.gpu.ffi.ColorPassDescriptor
import dev.chieppa.ease.gpu.ffi.Flags
import dev.chieppa.ease.gpu.ffi.createBuffer
import dev.chieppa.ease.gpu.ffi.createBufferBindGroup
import dev.chieppa.ease.gpu.ffi.createColorPipeline
import dev.chieppa.ease.gpu.ffi.createShaderModule
import dev.chieppa.webgpu.ffi.wgpu_h
import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

/** The shader's view uniform: scale x and y, offset x and y. */
private const val VIEW_BYTES = 16L

private const val SCALE_X = 0L
private const val SCALE_Y = 4L
private const val OFFSET_X = 8L
private const val OFFSET_Y = 12L
private const val VERTICES_PER_QUAD = 6
private const val LABEL = "quads"

/**
 * Clears a target to [clear] and draws up to [capacity] colored square quads on it in one instanced
 * draw: each quad is one instance the vertex shader reads from a storage buffer. Its buffers and
 * pass descriptor are made once, so recording a frame allocates no Kotlin objects.
 */
public class InstancedQuadPass(
  private val context: GpuContext,
  public val capacity: Int,
  public val clear: Color = DEFAULT_CLEAR,
) : AutoCloseable {
  init {
    require(capacity > 0) { "Needs room for at least 1 quad, got $capacity" }
  }

  private val arena = Arena.ofConfined()
  private val instanceBytes = capacity.toLong() * FLOATS_PER_QUAD * Float.SIZE_BYTES
  private val shader =
    createShaderModule(context.device, loadShader(), LABEL).also { context.checkErrors() }
  private val viewBuffer =
    createBuffer(context.device, VIEW_BYTES, Flags.BUFFER_UNIFORM or Flags.BUFFER_COPY_DST)
  private val instanceBuffer =
    createBuffer(context.device, instanceBytes, Flags.BUFFER_STORAGE or Flags.BUFFER_COPY_DST)
  private val viewStaging = arena.allocate(VIEW_BYTES)
  private val instanceStaging = arena.allocate(instanceBytes)
  private val passDescriptor = ColorPassDescriptor(arena, clear, LABEL)
  private val pipelines = arrayOfNulls<MemorySegment>(TextureFormat.entries.size)
  private val bindGroups = arrayOfNulls<MemorySegment>(TextureFormat.entries.size)

  /**
   * Records the pass into [encoder]: [target] cleared and every quad in [quads] drawn, placed by
   * [view]. Fails when [quads] holds more than [capacity].
   */
  public fun record(
    encoder: CommandEncoder,
    target: RenderTarget,
    view: View2D,
    quads: QuadInstances,
  ) {
    val count = quads.count
    require(count <= capacity) { "$count quads do not fit a pass for $capacity" }
    val format = target.format.ordinal
    val pipeline = pipelines[format] ?: createPipeline(target.format)
    writeView(view)
    if (count > 0) writeInstances(quads)
    val pass = passDescriptor.begin(encoder.handle, target.view)
    if (count > 0) {
      wgpu_h.wgpuRenderPassEncoderSetPipeline(pass, pipeline)
      wgpu_h.wgpuRenderPassEncoderSetBindGroup(pass, 0, bindGroups[format], 0, MemorySegment.NULL)
      wgpu_h.wgpuRenderPassEncoderDraw(pass, VERTICES_PER_QUAD, count, 0, 0)
    }
    wgpu_h.wgpuRenderPassEncoderEnd(pass)
    wgpu_h.wgpuRenderPassEncoderRelease(pass)
  }

  override fun close() {
    bindGroups.forEach { it?.let(wgpu_h::wgpuBindGroupRelease) }
    pipelines.forEach { it?.let(wgpu_h::wgpuRenderPipelineRelease) }
    wgpu_h.wgpuBufferRelease(instanceBuffer)
    wgpu_h.wgpuBufferRelease(viewBuffer)
    wgpu_h.wgpuShaderModuleRelease(shader)
    arena.close()
  }

  private fun writeView(view: View2D) {
    viewStaging.set(ValueLayout.JAVA_FLOAT, SCALE_X, view.scaleX)
    viewStaging.set(ValueLayout.JAVA_FLOAT, SCALE_Y, view.scaleY)
    viewStaging.set(ValueLayout.JAVA_FLOAT, OFFSET_X, view.offsetX)
    viewStaging.set(ValueLayout.JAVA_FLOAT, OFFSET_Y, view.offsetY)
    wgpu_h.wgpuQueueWriteBuffer(context.queue, viewBuffer, 0, viewStaging, VIEW_BYTES)
  }

  private fun writeInstances(quads: QuadInstances) {
    val floats = quads.count * FLOATS_PER_QUAD
    MemorySegment.copy(quads.data, 0, instanceStaging, ValueLayout.JAVA_FLOAT, 0, floats)
    val bytes = floats.toLong() * Float.SIZE_BYTES
    wgpu_h.wgpuQueueWriteBuffer(context.queue, instanceBuffer, 0, instanceStaging, bytes)
  }

  private fun createPipeline(format: TextureFormat): MemorySegment {
    val pipeline = createColorPipeline(context.device, shader, format.native, LABEL)
    val buffers =
      listOf(BufferBinding(viewBuffer, VIEW_BYTES), BufferBinding(instanceBuffer, instanceBytes))
    bindGroups[format.ordinal] = createBufferBindGroup(context.device, pipeline, buffers)
    pipelines[format.ordinal] = pipeline
    context.checkErrors()
    return pipeline
  }

  public companion object {
    /** The color the pass clears to unless told otherwise: a dark slate. */
    public val DEFAULT_CLEAR: Color = Color(0.08f, 0.09f, 0.12f)

    private fun loadShader(): String =
      checkNotNull(InstancedQuadPass::class.java.getResource("quads.wgsl")) { "quads.wgsl" }
        .readText()
  }
}

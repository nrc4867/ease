package dev.chieppa.ease.gpu.lit

import dev.chieppa.ease.gpu.CommandEncoder
import dev.chieppa.ease.gpu.GpuContext
import dev.chieppa.ease.gpu.RenderTarget
import dev.chieppa.ease.gpu.TextureFormat
import dev.chieppa.ease.gpu.ffi.DepthPassDescriptor
import dev.chieppa.ease.gpu.ffi.Flags
import dev.chieppa.ease.gpu.ffi.FrameCalls
import dev.chieppa.ease.gpu.ffi.VertexAttribute
import dev.chieppa.ease.gpu.ffi.VertexLayout
import dev.chieppa.ease.gpu.ffi.createBuffer
import dev.chieppa.ease.gpu.ffi.createDepthPipeline
import dev.chieppa.ease.gpu.ffi.createShaderModule
import dev.chieppa.ease.gpu.mesh.GpuMesh
import dev.chieppa.ease.gpu.mesh.MeshBuffers
import dev.chieppa.ease.gpu.mesh.NORMAL_OFFSET
import dev.chieppa.ease.gpu.mesh.POSITION_OFFSET
import dev.chieppa.ease.gpu.mesh.UV_OFFSET
import dev.chieppa.ease.gpu.mesh.VERTEX_BYTES
import dev.chieppa.ease.memory.NativeWriter
import dev.chieppa.webgpu.ffi.wgpu_h
import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment

private const val LABEL = "lit meshes"
private const val UNIFORM = Flags.BUFFER_UNIFORM or Flags.BUFFER_COPY_DST
private const val STORAGE = Flags.BUFFER_STORAGE or Flags.BUFFER_COPY_DST

/** The vertex layout of every [dev.chieppa.ease.gpu.mesh.MeshData]. */
private val MESH_VERTICES =
  VertexLayout(
    VERTEX_BYTES,
    listOf(
      VertexAttribute(3, POSITION_OFFSET),
      VertexAttribute(3, NORMAL_OFFSET),
      VertexAttribute(2, UV_OFFSET),
    ),
  )

/**
 * Draws a [MeshFrame] of up to [capacity] meshes with one material [template], lit by the sun and
 * an ambient term in a forward pass with a depth buffer: the target cleared, then each draw's mesh
 * drawn with its transforms, material and textures. Uniforms and per-draw data are written through
 * generated struct writers into the context's frame arena and handed to the queue from there;
 * meshes upload from their own native memory when they change, and computed meshes draw from their
 * own buffers. Its buffers, bind groups, pass descriptor and depth texture are made once (the depth
 * texture again when the target's size changes), so recording a steady frame allocates nothing on
 * the heap. Its host calls [GpuContext.beginFrame] before each frame it records, and opens the
 * context with room in its frame arena for [capacity] draws.
 *
 * Its pipelines use one explicit layout ([LitGroups]): group 0 the frame and the view, group 1 the
 * textures of a [MaterialTextures] set, group 2 the materials, group 3 the objects. A draw samples
 * the set [MeshFrame.textures] gives it, or [defaultTextures]; the pass rebinds group 1 only
 * between draws whose sets differ, so draws sorted by set bind least.
 */
public class LitMeshPass(
  private val context: GpuContext,
  public val capacity: Int,
  template: MaterialTemplate = MaterialTemplate.LIT,
) : AutoCloseable {
  init {
    require(capacity > 0) { "Needs room for at least 1 draw, got $capacity" }
    val needed = FrameUploads.bytesPerFrame(capacity)
    val held = context.frames.bytesPerFrame
    require(needed <= held) {
      "A lit pass for $capacity draws writes $needed bytes a frame, more than the $held the " +
        "context's frame arena holds: open the context with bytesPerFrame of at least $needed"
    }
  }

  private val arena = Arena.ofConfined()
  private val shader =
    createShaderModule(context.device, forwardShader(template), LABEL).also {
      context.checkErrors()
    }
  private val frameBuffer = createBuffer(context.device, FrameDataWriter.STRUCT_SIZE, UNIFORM)
  private val viewBuffer = createBuffer(context.device, ViewDataWriter.STRUCT_SIZE, UNIFORM)
  private val materialBytes = capacity * LitMaterialWriter.STRUCT_SIZE
  private val objectBytes = capacity * ObjectDataWriter.STRUCT_SIZE
  private val materialBuffer = createBuffer(context.device, materialBytes, STORAGE)
  private val objectBuffer = createBuffer(context.device, objectBytes, STORAGE)
  private val groups =
    LitGroups(
      context,
      frameBuffer,
      viewBuffer,
      materialBuffer,
      materialBytes,
      objectBuffer,
      objectBytes,
    )
  private val passDescriptor = DepthPassDescriptor(arena, LABEL)
  private val pipelines = arrayOfNulls<MemorySegment>(TextureFormat.entries.size)
  private val gpuMeshes = arrayOfNulls<MeshBuffers>(capacity)
  private val depth = DepthBuffer(context)
  private val writer = NativeWriter()
  private val uploads = FrameUploads(context, writer)

  /** The set a draw without textures samples: every slot its default. */
  public val defaultTextures: MaterialTextures = MaterialTextures(context)

  /** Records the pass into [encoder]: [target] cleared and every draw of [frame] drawn. */
  public fun record(encoder: CommandEncoder, target: RenderTarget, frame: MeshFrame) {
    val count = frame.count
    require(count <= capacity) { "$count draws do not fit a pass for $capacity" }
    val pipeline = pipelines[target.format.ordinal] ?: createPipeline(target.format)
    for (index in 0 until count) gpuMeshes[index] = buffersOf(frame, index)
    uploads.frame(frameBuffer, frame)
    uploads.view(viewBuffer, frame)
    uploads.draws(materialBuffer, objectBuffer, frame)
    val depthView = depth.viewFor(target.size)
    val pass = passDescriptor.begin(encoder.address, target.view, depthView, frame.clear)
    FrameCalls.setPipeline(pass, pipeline)
    FrameCalls.setBindGroup(pass, LitGroups.FRAME, groups.frame)
    FrameCalls.setBindGroup(pass, LitGroups.MATERIALS, groups.materials)
    FrameCalls.setBindGroup(pass, LitGroups.OBJECTS, groups.objects)
    var bound: MaterialTextures? = null
    for (index in 0 until count) {
      val textures = frame.textures(index) ?: defaultTextures
      if (textures !== bound) {
        FrameCalls.setBindGroup(pass, LitGroups.TEXTURES, textures.group)
        bound = textures
      }
      draw(pass, index)
    }
    FrameCalls.endPass(pass)
  }

  private fun buffersOf(frame: MeshFrame, index: Int): MeshBuffers {
    val computed = frame.computed[index]
    if (computed != null) {
      check(!computed.closed) { "Draw $index's computed mesh is closed" }
      require(computed.context === context) {
        "Draw $index's computed mesh lives on another device than this pass"
      }
      return computed.buffers
    }
    return GpuMesh.of(context, checkNotNull(frame.meshes[index]) { "No draw $index" })
  }

  private fun draw(pass: Long, index: Int) {
    val mesh = checkNotNull(gpuMeshes[index])
    if (mesh.indexCount == 0) return
    FrameCalls.setVertexBuffer(pass, mesh.vertexBuffer, mesh.vertexBytes)
    FrameCalls.setIndexBuffer(pass, mesh.indexBuffer, mesh.indexBytes)
    FrameCalls.drawIndexed(pass, mesh.indexCount, index)
  }

  override fun close() {
    defaultTextures.close()
    pipelines.forEach { it?.let(wgpu_h::wgpuRenderPipelineRelease) }
    groups.close()
    listOf(frameBuffer, viewBuffer, materialBuffer, objectBuffer).forEach(wgpu_h::wgpuBufferRelease)
    wgpu_h.wgpuShaderModuleRelease(shader)
    depth.close()
    arena.close()
  }

  private fun createPipeline(format: TextureFormat): MemorySegment {
    val pipeline =
      createDepthPipeline(
        context.device,
        shader,
        format.native,
        MESH_VERTICES,
        groups.pipelineLayout,
        LABEL,
      )
    pipelines[format.ordinal] = pipeline
    context.checkErrors()
    return pipeline
  }
}

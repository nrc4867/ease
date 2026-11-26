package dev.chieppa.ease.gpu.ffi

import dev.chieppa.webgpu.ffi.WGPUColorTargetState
import dev.chieppa.webgpu.ffi.WGPUDepthStencilState
import dev.chieppa.webgpu.ffi.WGPUFragmentState
import dev.chieppa.webgpu.ffi.WGPUMultisampleState
import dev.chieppa.webgpu.ffi.WGPUPrimitiveState
import dev.chieppa.webgpu.ffi.WGPURenderPipelineDescriptor
import dev.chieppa.webgpu.ffi.WGPUShaderModuleDescriptor
import dev.chieppa.webgpu.ffi.WGPUShaderSourceWGSL
import dev.chieppa.webgpu.ffi.WGPUVertexAttribute
import dev.chieppa.webgpu.ffi.WGPUVertexBufferLayout
import dev.chieppa.webgpu.ffi.WGPUVertexState
import dev.chieppa.webgpu.ffi.wgpu_h
import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment

/** Every sample of a pixel, webgpu.h's default multisample mask. */
private const val ALL_SAMPLES = -1

/** Writes to every color channel, webgpu.h's WGPUColorWriteMask_All. */
private const val WRITE_ALL = 0xF

/** A shader module compiled from [wgsl] on [device]. */
internal fun createShaderModule(device: MemorySegment, wgsl: String, label: String): MemorySegment =
  Arena.ofConfined().use { arena ->
    val source = arena.struct(WGPUShaderSourceWGSL.layout())
    chain(source, wgpu_h.WGPUSType_ShaderSourceWGSL())
    arena.setString(WGPUShaderSourceWGSL.code(source), wgsl)
    val descriptor = arena.struct(WGPUShaderModuleDescriptor.layout())
    WGPUShaderModuleDescriptor.nextInChain(descriptor, source)
    arena.setString(WGPUShaderModuleDescriptor.label(descriptor), label)
    wgpu_h.wgpuDeviceCreateShaderModule(device, descriptor)
  }

/**
 * A render pipeline drawing triangles with [shader]'s `vertex_main` and `fragment_main` into one
 * color target of [format], single-sampled, no depth, no vertex buffers, with a layout wgpu derives
 * from the shader.
 */
internal fun createColorPipeline(
  device: MemorySegment,
  shader: MemorySegment,
  format: Int,
  label: String,
): MemorySegment =
  Arena.ofConfined().use { arena ->
    val descriptor = arena.struct(WGPURenderPipelineDescriptor.layout())
    arena.setString(WGPURenderPipelineDescriptor.label(descriptor), label)
    val vertex = WGPURenderPipelineDescriptor.vertex(descriptor)
    WGPUVertexState.module(vertex, shader)
    arena.setString(WGPUVertexState.entryPoint(vertex), "vertex_main")
    val primitive = WGPURenderPipelineDescriptor.primitive(descriptor)
    WGPUPrimitiveState.topology(primitive, wgpu_h.WGPUPrimitiveTopology_TriangleList())
    WGPUPrimitiveState.frontFace(primitive, wgpu_h.WGPUFrontFace_CCW())
    WGPUPrimitiveState.cullMode(primitive, wgpu_h.WGPUCullMode_None())
    val multisample = WGPURenderPipelineDescriptor.multisample(descriptor)
    WGPUMultisampleState.count(multisample, 1)
    WGPUMultisampleState.mask(multisample, ALL_SAMPLES)
    WGPURenderPipelineDescriptor.fragment(descriptor, fragment(arena, shader, format))
    wgpu_h.wgpuDeviceCreateRenderPipeline(device, descriptor)
  }

/**
 * A render pipeline drawing triangles with [shader]'s `vertex_main` and `fragment_main` from one
 * vertex buffer of [vertices] into one color target of [format] and a depth buffer, nearer
 * fragments winning; counter-clockwise faces are the front and back faces are culled, with the
 * pipeline [layout] given.
 */
@Suppress("LongParameterList") // One pipeline description, as wgpu takes it.
internal fun createDepthPipeline(
  device: MemorySegment,
  shader: MemorySegment,
  format: Int,
  vertices: VertexLayout,
  layout: MemorySegment,
  label: String,
): MemorySegment =
  Arena.ofConfined().use { arena ->
    val descriptor = arena.struct(WGPURenderPipelineDescriptor.layout())
    arena.setString(WGPURenderPipelineDescriptor.label(descriptor), label)
    WGPURenderPipelineDescriptor.layout(descriptor, layout)
    val vertex = WGPURenderPipelineDescriptor.vertex(descriptor)
    WGPUVertexState.module(vertex, shader)
    arena.setString(WGPUVertexState.entryPoint(vertex), "vertex_main")
    WGPUVertexState.bufferCount(vertex, 1)
    WGPUVertexState.buffers(vertex, vertexBuffer(arena, vertices))
    val primitive = WGPURenderPipelineDescriptor.primitive(descriptor)
    WGPUPrimitiveState.topology(primitive, wgpu_h.WGPUPrimitiveTopology_TriangleList())
    WGPUPrimitiveState.frontFace(primitive, wgpu_h.WGPUFrontFace_CCW())
    WGPUPrimitiveState.cullMode(primitive, wgpu_h.WGPUCullMode_Back())
    val depth = arena.struct(WGPUDepthStencilState.layout())
    WGPUDepthStencilState.format(depth, DEPTH_FORMAT)
    WGPUDepthStencilState.depthWriteEnabled(depth, wgpu_h.WGPUOptionalBool_True())
    WGPUDepthStencilState.depthCompare(depth, wgpu_h.WGPUCompareFunction_Less())
    WGPURenderPipelineDescriptor.depthStencil(descriptor, depth)
    val multisample = WGPURenderPipelineDescriptor.multisample(descriptor)
    WGPUMultisampleState.count(multisample, 1)
    WGPUMultisampleState.mask(multisample, ALL_SAMPLES)
    WGPURenderPipelineDescriptor.fragment(descriptor, fragment(arena, shader, format))
    wgpu_h.wgpuDeviceCreateRenderPipeline(device, descriptor)
  }

private fun vertexBuffer(arena: Arena, vertices: VertexLayout): MemorySegment {
  val count = vertices.attributes.size.toLong()
  val attributes = arena.allocate(WGPUVertexAttribute.layout(), count)
  vertices.attributes.forEachIndexed { location, attribute ->
    val entry = WGPUVertexAttribute.asSlice(attributes, location.toLong())
    WGPUVertexAttribute.format(entry, floatFormat(attribute.components))
    WGPUVertexAttribute.offset(entry, attribute.offset)
    WGPUVertexAttribute.shaderLocation(entry, location)
  }
  val layout = arena.struct(WGPUVertexBufferLayout.layout())
  WGPUVertexBufferLayout.stepMode(layout, wgpu_h.WGPUVertexStepMode_Vertex())
  WGPUVertexBufferLayout.arrayStride(layout, vertices.stride)
  WGPUVertexBufferLayout.attributeCount(layout, count)
  WGPUVertexBufferLayout.attributes(layout, attributes)
  return layout
}

@Suppress("MagicNumber") // Component counts, named by the formats they pick.
private fun floatFormat(components: Int): Int =
  when (components) {
    2 -> wgpu_h.WGPUVertexFormat_Float32x2()
    3 -> wgpu_h.WGPUVertexFormat_Float32x3()
    4 -> wgpu_h.WGPUVertexFormat_Float32x4()
    else -> error("A vertex attribute has 2 to 4 floats, not $components")
  }

private fun fragment(arena: Arena, shader: MemorySegment, format: Int): MemorySegment {
  val target = arena.struct(WGPUColorTargetState.layout())
  WGPUColorTargetState.format(target, format)
  WGPUColorTargetState.writeMask(target, WRITE_ALL.toLong())
  val fragment = arena.struct(WGPUFragmentState.layout())
  WGPUFragmentState.module(fragment, shader)
  arena.setString(WGPUFragmentState.entryPoint(fragment), "fragment_main")
  WGPUFragmentState.targetCount(fragment, 1)
  WGPUFragmentState.targets(fragment, target)
  return fragment
}

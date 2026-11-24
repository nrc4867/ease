package dev.chieppa.ease.gpu.ffi

import dev.chieppa.webgpu.ffi.WGPUColorTargetState
import dev.chieppa.webgpu.ffi.WGPUFragmentState
import dev.chieppa.webgpu.ffi.WGPUMultisampleState
import dev.chieppa.webgpu.ffi.WGPUPrimitiveState
import dev.chieppa.webgpu.ffi.WGPURenderPipelineDescriptor
import dev.chieppa.webgpu.ffi.WGPUShaderModuleDescriptor
import dev.chieppa.webgpu.ffi.WGPUShaderSourceWGSL
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
    val target = arena.struct(WGPUColorTargetState.layout())
    WGPUColorTargetState.format(target, format)
    WGPUColorTargetState.writeMask(target, WRITE_ALL.toLong())
    val fragment = arena.struct(WGPUFragmentState.layout())
    WGPUFragmentState.module(fragment, shader)
    arena.setString(WGPUFragmentState.entryPoint(fragment), "fragment_main")
    WGPUFragmentState.targetCount(fragment, 1)
    WGPUFragmentState.targets(fragment, target)
    WGPURenderPipelineDescriptor.fragment(descriptor, fragment)
    wgpu_h.wgpuDeviceCreateRenderPipeline(device, descriptor)
  }

package dev.chieppa.ease.gpu.lit

import dev.chieppa.ease.gpu.GpuContext
import dev.chieppa.ease.gpu.ffi.Binding
import dev.chieppa.ease.gpu.ffi.createBindGroup
import dev.chieppa.ease.gpu.texture.GpuTexture
import dev.chieppa.webgpu.ffi.wgpu_h
import java.lang.foreign.MemorySegment

/**
 * The textures draws of a lit pass sample, one per [TextureSlot], given to draws with
 * [MeshFrame.textures]. Empty slots take the defaults, which leave the material's own values as
 * they are: white, or a flat normal. Each texture is read with its own filter and wrap. The set is
 * one bind group of the device's texture layout, so every lit pass on [context] can draw with it.
 * Close it before the textures it holds.
 */
public class MaterialTextures(
  context: GpuContext,
  baseColor: GpuTexture? = null,
  normal: GpuTexture? = null,
  metallicRoughness: GpuTexture? = null,
  emissive: GpuTexture? = null,
  occlusion: GpuTexture? = null,
) : AutoCloseable {
  private val textures: List<GpuTexture>

  internal val group: MemorySegment

  init {
    val given = listOf(baseColor, normal, metallicRoughness, emissive, occlusion)
    val bindings = context.textureBindings
    textures = TextureSlot.entries.map { given[it.ordinal] ?: bindings.default(it) }
    val entries =
      textures.map { Binding.Texture(it.view) } + textures.map { Binding.Sampler(it.sampler) }
    group = createBindGroup(context.device, bindings.layout, entries)
  }

  /** The texture in [slot]: the one given, or the default. */
  public operator fun get(slot: TextureSlot): GpuTexture = textures[slot.ordinal]

  override fun close() {
    wgpu_h.wgpuBindGroupRelease(group)
  }
}

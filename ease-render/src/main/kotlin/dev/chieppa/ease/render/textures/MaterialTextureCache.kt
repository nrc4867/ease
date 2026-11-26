package dev.chieppa.ease.render.textures

import dev.chieppa.ease.assets.loading.AssetLoadException
import dev.chieppa.ease.assets.loading.AssetStore
import dev.chieppa.ease.gpu.GpuContext
import dev.chieppa.ease.gpu.lit.MaterialTextures
import dev.chieppa.ease.gpu.texture.GpuTexture
import dev.chieppa.ease.render.assets.MaterialAsset
import dev.chieppa.ease.render.assets.TextureSlot
import dev.chieppa.ease.textures.CookedTexture
import kotlin.reflect.KClass

/**
 * The [MaterialTextures] set of each material asset drawn, made on [context] from the cooked
 * textures its slots name, loaded through [store] and put on the GPU with [TextureUpload]. A set is
 * made the first frame its asset is drawn and kept while it is drawn; a set no draw asked for
 * during a frame is closed, with its textures, at [endFrame], as happens to a reloaded material's
 * old set. A texture that does not load leaves its slot at the default. Looking up a set already
 * made allocates nothing. Not shared between threads.
 */
public class MaterialTextureCache(private val context: GpuContext, private val store: AssetStore) :
  AutoCloseable {
  private val assets = ArrayList<MaterialAsset>()
  private val sets = ArrayList<Entry>()

  /** Why a texture did not load, one line each, for the latest failure of each. */
  public val problems: MutableList<String> = mutableListOf()

  private class Entry(val textures: MaterialTextures, val owned: List<GpuTexture>) {
    var used = true
  }

  /** The set [asset] draws with, or null when it names no texture. */
  public fun of(asset: MaterialAsset): MaterialTextures? {
    val known = indexOf(asset)
    val entry =
      when {
        known >= 0 -> sets[known].also { it.used = true }
        hasTextures(asset) ->
          make(asset).also {
            assets += asset
            sets += it
          }
        else -> null
      }
    return entry?.textures
  }

  /** Where [asset] is among those with a set, by identity, or -1. */
  private fun indexOf(asset: MaterialAsset): Int {
    var found = -1
    for (index in assets.indices) if (assets[index] === asset) found = index
    return found
  }

  /** Closes the sets no draw asked for since the last call. */
  public fun endFrame() {
    var index = sets.size - 1
    while (index >= 0) {
      val entry = sets[index]
      if (!entry.used) {
        release(entry)
        sets.removeAt(index)
        assets.removeAt(index)
      } else {
        entry.used = false
      }
      index--
    }
  }

  override fun close() {
    sets.forEach(::release)
    sets.clear()
    assets.clear()
  }

  private fun hasTextures(asset: MaterialAsset): Boolean =
    asset.baseColorTexture != null ||
      asset.normalTexture != null ||
      asset.metallicRoughnessTexture != null ||
      asset.emissiveTexture != null ||
      asset.occlusionTexture != null

  private fun make(asset: MaterialAsset): Entry {
    val owned = mutableListOf<GpuTexture>()
    fun upload(slot: TextureSlot?): GpuTexture? =
      slot?.let { load(it) }?.let { TextureUpload.upload(context, it) }?.also { owned += it }
    val textures =
      MaterialTextures(
        context,
        baseColor = upload(asset.baseColorTexture),
        normal = upload(asset.normalTexture),
        metallicRoughness = upload(asset.metallicRoughnessTexture),
        emissive = upload(asset.emissiveTexture),
        occlusion = upload(asset.occlusionTexture),
      )
    return Entry(textures, owned)
  }

  private fun load(slot: TextureSlot): CookedTexture? =
    try {
      store.handle(slot.texture, COOKED).value
    } catch (failure: AssetLoadException) {
      problems += failure.message.orEmpty()
      null
    }

  private fun release(entry: Entry) {
    entry.textures.close()
    entry.owned.forEach(GpuTexture::close)
  }

  private companion object {
    val COOKED: KClass<CookedTexture> = CookedTexture::class
  }
}

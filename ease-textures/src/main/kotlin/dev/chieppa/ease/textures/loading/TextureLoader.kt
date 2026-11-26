package dev.chieppa.ease.textures.loading

import dev.chieppa.ease.assets.Guid
import dev.chieppa.ease.assets.loading.AssetLoader
import dev.chieppa.ease.textures.CookedTexture
import dev.chieppa.ease.textures.ktx2.Ktx2Reader
import kotlin.reflect.KClass

/**
 * Loads KTX2 files as [CookedTexture]s: the textures the texture importer cooks, texture sub-assets
 * other importers cook, and KTX2 files placed in the content as they are.
 */
public object TextureLoader : AssetLoader<CookedTexture> {
  override val type: KClass<CookedTexture> = CookedTexture::class

  override fun load(guid: Guid, bytes: ByteArray): CookedTexture = Ktx2Reader.read(bytes)
}

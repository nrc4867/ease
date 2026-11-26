package dev.chieppa.ease.textures

import dev.chieppa.ease.assets.associations.FileAssociations
import dev.chieppa.ease.assets.associations.FileMatch
import dev.chieppa.ease.assets.importing.Importer
import dev.chieppa.ease.assets.loading.AssetLoader
import dev.chieppa.ease.textures.importing.TextureImporter
import dev.chieppa.ease.textures.loading.TextureLoader

/** Where Ease's texture contributors say they come from. */
public const val TEXTURES_ORIGIN: String = "ease-textures"

/**
 * Registers Ease's texture contributors: the texture importer for every image extension it reads
 * ([registerTextureImporter]) and the KTX2 loader ([registerTextureLoader]).
 */
public fun FileAssociations.registerTextures() {
  registerTextureImporter()
  registerTextureLoader()
}

/** Registers the texture importer for every image extension it reads, for tools that import. */
public fun FileAssociations.registerTextureImporter() {
  for (extension in TextureImporter.extensions) {
    register(Importer.kind, FileMatch.Extension(extension), 0, TextureImporter, TEXTURES_ORIGIN)
  }
}

/** Registers the KTX2 loader for `.ktx2` files, cooked or placed in the content as they are. */
public fun FileAssociations.registerTextureLoader() {
  register(AssetLoader.kind, FileMatch.Extension("ktx2"), 0, TextureLoader, TEXTURES_ORIGIN)
}

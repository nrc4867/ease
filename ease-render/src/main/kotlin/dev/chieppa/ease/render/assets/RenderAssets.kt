package dev.chieppa.ease.render.assets

import dev.chieppa.ease.assets.associations.FileAssociations
import dev.chieppa.ease.assets.associations.FileMatch
import dev.chieppa.ease.assets.associations.Registration
import dev.chieppa.ease.assets.loading.AssetLoader

/** Where render asset contributors say they came from. */
private const val ORIGIN = "ease-render"

/** The priority render loaders register at. */
private const val PRIORITY = 0

/** The cooked mesh extension. */
public const val MESH_EXTENSION: String = "mesh"

/** The material asset extension. */
public const val MATERIAL_EXTENSION: String = "material"

/**
 * Registers the loaders of render assets with the file association service: cooked meshes (`.mesh`)
 * and material assets (`.material`), whether they are files of their own or an importer's cooked
 * assets. Close the returned handles to remove them.
 */
public fun FileAssociations.registerRenderAssets(): List<Registration> =
  listOf<Pair<String, AssetLoader<*>>>(
      MESH_EXTENSION to MeshLoader,
      MATERIAL_EXTENSION to MaterialFormat,
    )
    .map { (extension, loader) ->
      register(AssetLoader.kind, FileMatch.Extension(extension), PRIORITY, loader, ORIGIN)
    }

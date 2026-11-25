package dev.chieppa.ease.scene.assets

import dev.chieppa.ease.assets.associations.FileAssociations
import dev.chieppa.ease.assets.associations.FileMatch
import dev.chieppa.ease.assets.associations.FolderAsset
import dev.chieppa.ease.assets.associations.Registration
import dev.chieppa.ease.assets.loading.AssetLoader
import dev.chieppa.ease.scene.files.SceneFormat

/** Where scene contributors say they came from. */
private const val ORIGIN = "ease-scene"

/** The priority scene contributors register at. */
private const val PRIORITY = 0

/** Marks a scene or prefab folder as one asset. */
private object SceneFolderAsset : FolderAsset

/**
 * Registers scenes and prefabs with the file association service: `.scene` and `.prefab` folders
 * are each one asset, with the meta file beside the folder, and [SceneLoader] loads them. Register
 * before loading the asset database, so it reads those folders as assets. Close the returned
 * handles to remove them.
 */
public fun FileAssociations.registerScenes(): List<Registration> =
  listOf(SceneFormat.SCENE_EXTENSION, SceneFormat.PREFAB_EXTENSION).flatMap { extension ->
    val match = FileMatch.Extension(extension)
    listOf(
      register(FolderAsset.kind, match, PRIORITY, SceneFolderAsset, ORIGIN),
      register(AssetLoader.kind, match, PRIORITY, SceneLoader, ORIGIN),
    )
  }

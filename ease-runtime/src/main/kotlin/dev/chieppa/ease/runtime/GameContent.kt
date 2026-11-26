package dev.chieppa.ease.runtime

import dev.chieppa.ease.animation.assets.registerAnimationAssets
import dev.chieppa.ease.assets.associations.ContributorProvider
import dev.chieppa.ease.assets.associations.FileAssociationRegistry
import dev.chieppa.ease.assets.database.AssetDatabase
import dev.chieppa.ease.assets.importing.ImportService
import dev.chieppa.ease.assets.loading.AssetStore
import dev.chieppa.ease.platform.files.FileWatcher
import dev.chieppa.ease.platform.files.JdkFileWatcher
import dev.chieppa.ease.render.assets.registerRenderAssets
import dev.chieppa.ease.scene.assets.registerScenes
import dev.chieppa.ease.textures.registerTextureImporter
import dev.chieppa.ease.textures.registerTextureLoader
import java.nio.file.Files
import java.nio.file.Path

/** Opens a game's content folder for a host: the assets its worlds load, scenes among them. */
public object GameContent {
  /**
   * The assets under [root], scene and prefab folders and KTX2 textures included, watched by
   * [watcher] so a changed file reloads in the running game at the next tick. The importers of
   * every `ContributorProvider` on [classLoader] (the engine's glTF importer and the game's own)
   * and the texture importer run over it at once, and cooked assets load from their results; a
   * source that fails to import is listed in the store's `imports.problems` and its assets do not
   * load. Throws `AssetDatabaseRejected`, naming each file, when the folder has a GUID twice, a
   * meta file without a source or a source without one.
   */
  public fun open(
    root: Path,
    watcher: FileWatcher = JdkFileWatcher(),
    classLoader: ClassLoader = checkNotNull(GameContent::class.java.classLoader),
  ): AssetStore {
    val associations = FileAssociationRegistry()
    associations.registerScenes()
    associations.registerTextureImporter()
    associations.registerTextureLoader()
    associations.registerRenderAssets()
    associations.registerAnimationAssets()
    val database = AssetDatabase.load(root, watcher, associations = associations)
    ContributorProvider.loadAll(classLoader).forEach { it.registerContributors(associations) }
    val imports = ImportService(database, associations)
    imports.refresh()
    return AssetStore(database, associations, imports)
  }

  /**
   * The content of [module] a host was given as [folder], opened with [watcher], or null when the
   * game needs none. Throws at once, before the host starts anything, when the game starts from a
   * scene and no folder was given, or the folder does not exist.
   */
  public fun forGame(
    module: GameModule,
    folder: Path?,
    watcher: FileWatcher = JdkFileWatcher(),
  ): AssetStore? {
    val name = module.javaClass.simpleName
    val scene = module.startScene()
    check(folder != null || scene == null) {
      "$name starts from scene $scene: pass its content folder with --content <folder>"
    }
    if (folder == null) return null
    check(Files.isDirectory(folder)) { "$name's content folder $folder does not exist" }
    return open(folder, watcher)
  }
}

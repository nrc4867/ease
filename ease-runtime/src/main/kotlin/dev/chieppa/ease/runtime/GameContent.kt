package dev.chieppa.ease.runtime

import dev.chieppa.ease.assets.associations.FileAssociationRegistry
import dev.chieppa.ease.assets.database.AssetDatabase
import dev.chieppa.ease.assets.loading.AssetStore
import dev.chieppa.ease.platform.files.FileWatcher
import dev.chieppa.ease.platform.files.JdkFileWatcher
import dev.chieppa.ease.scene.assets.registerScenes
import dev.chieppa.ease.textures.registerTextureLoader
import java.nio.file.Files
import java.nio.file.Path

/** Opens a game's content folder for a host: the assets its worlds load, scenes among them. */
public object GameContent {
  /**
   * The assets under [root], scene and prefab folders and KTX2 textures included, watched by
   * [watcher] so a changed file reloads in the running game at the next tick. Throws
   * `AssetDatabaseRejected`, naming each file, when the folder has a GUID twice, a meta file
   * without a source or a source without one.
   */
  public fun open(root: Path, watcher: FileWatcher = JdkFileWatcher()): AssetStore {
    val associations = FileAssociationRegistry()
    associations.registerScenes()
    associations.registerTextureLoader()
    val database = AssetDatabase.load(root, watcher, associations = associations)
    return AssetStore(database, associations)
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

package dev.chieppa.ease.runtime

import dev.chieppa.ease.assets.associations.FileAssociationRegistry
import dev.chieppa.ease.assets.database.AssetDatabase
import dev.chieppa.ease.assets.loading.AssetStore
import dev.chieppa.ease.platform.files.FileWatcher
import dev.chieppa.ease.platform.files.JdkFileWatcher
import dev.chieppa.ease.scene.assets.registerScenes
import java.nio.file.Path

/** Opens a game's content folder for a host: the assets its worlds load, scenes among them. */
public object GameContent {
  /**
   * The assets under [root], scene and prefab folders included, watched by [watcher] so a changed
   * file reloads in the running game at the next tick. Throws `AssetDatabaseRejected`, naming each
   * file, when the folder has a GUID twice, a meta file without a source or a source without one.
   */
  public fun open(root: Path, watcher: FileWatcher = JdkFileWatcher()): AssetStore {
    val associations = FileAssociationRegistry()
    associations.registerScenes()
    val database = AssetDatabase.load(root, watcher, associations = associations)
    return AssetStore(database, associations)
  }
}

package dev.chieppa.ease.importers.gltf

import dev.chieppa.ease.animation.assets.registerAnimationAssets
import dev.chieppa.ease.assets.Guid
import dev.chieppa.ease.assets.associations.FileAssociationRegistry
import dev.chieppa.ease.assets.database.AssetDatabase
import dev.chieppa.ease.assets.importing.ImportService
import dev.chieppa.ease.assets.loading.AssetStore
import dev.chieppa.ease.assets.meta.AssetMeta
import dev.chieppa.ease.assets.meta.MetaFiles
import dev.chieppa.ease.importers.EngineImporters
import dev.chieppa.ease.platform.files.ManualFileWatcher
import dev.chieppa.ease.render.assets.registerRenderAssets
import dev.chieppa.ease.scene.assets.registerScenes
import dev.chieppa.ease.textures.registerTextureLoader
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.serialization.json.JsonObject

/**
 * A content folder for tests: test-resource files copied in with meta files, the engine's importers
 * and the render, texture and scene loaders registered, and every source imported.
 */
internal class ImportedProject(private val root: Path) {
  private var next = 0L
  val associations = FileAssociationRegistry()

  /** The watcher the database listens to; tests publish their file changes through it. */
  val watcher = ManualFileWatcher()

  init {
    associations.registerScenes()
    associations.registerRenderAssets()
    associations.registerAnimationAssets()
    associations.registerTextureLoader()
    EngineImporters().registerContributors(associations)
  }

  /**
   * Copies the test resource [resource] in as [name] with a meta file holding [settings], and
   * returns its GUID.
   */
  fun add(
    resource: String,
    name: String = resource.substringAfterLast('/'),
    settings: JsonObject? = null,
  ): Guid {
    val guid = Guid(0, ++next)
    val file = root.resolve(name)
    Files.createDirectories(file.parent)
    checkNotNull(javaClass.getResourceAsStream("/$resource")) { resource }
      .use {
        Files.copy(it, file)
      }
    MetaFiles.write(MetaFiles.metaOf(file), AssetMeta(guid = guid, settings = settings))
    return guid
  }

  /** Writes [text] in as [name] with a meta file, and returns its GUID. */
  fun write(name: String, text: String): Guid {
    val guid = Guid(0, ++next)
    val file = root.resolve(name)
    Files.createDirectories(file.parent)
    Files.writeString(file, text)
    MetaFiles.write(MetaFiles.metaOf(file), AssetMeta(guid = guid))
    return guid
  }

  /** The database and import service of the folder, every source imported once. */
  fun open(): Pair<AssetStore, ImportService> {
    val database = AssetDatabase.open(root, watcher, associations = associations)
    val imports = ImportService(database, associations)
    imports.refresh()
    return AssetStore(database, associations, imports) to imports
  }
}

package dev.chieppa.ease.project

import dev.chieppa.ease.assets.Guid
import dev.chieppa.ease.assets.meta.AssetMeta
import dev.chieppa.ease.assets.meta.MetaFiles
import dev.chieppa.ease.project.layout.ProjectLayout
import dev.chieppa.ease.scene.document.SceneDocument
import dev.chieppa.ease.scene.document.SceneNode
import dev.chieppa.ease.scene.files.SceneFolder
import dev.chieppa.ease.scene.node.LocalId
import dev.chieppa.ease.scene.node.OrderKey
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Writes the files of a project in a test folder: assets with sidecars, and scenes. */
class TestProject(val root: Path) {
  private val content = ProjectLayout.content(root)

  /**
   * Writes the asset [path] (inside the content folder) holding [text], with a sidecar of [guid].
   */
  fun asset(path: String, text: String, guid: Guid): Guid {
    val file = content.resolve(path)
    Files.createDirectories(file.parent)
    Files.writeString(file, text)
    MetaFiles.write(MetaFiles.metaOf(file), AssetMeta(guid = guid))
    return guid
  }

  /** Writes the scene folder [path] (inside the content folder), with a sidecar of [guid]. */
  fun scene(path: String, guid: Guid, nodes: List<SceneNode>): Guid {
    val folder = content.resolve(path)
    SceneFolder.write(folder, SceneDocument(nodes = nodes))
    MetaFiles.write(MetaFiles.metaOf(folder), AssetMeta(guid = guid))
    return guid
  }

  /** The scene folder [path] as it is on disk. */
  fun readScene(path: String): SceneDocument = SceneFolder.read(content.resolve(path))

  /** Replaces the scene folder [path] by [nodes]. */
  fun saveScene(path: String, nodes: List<SceneNode>) {
    SceneFolder.write(content.resolve(path), SceneDocument(nodes = nodes))
  }

  /** The file [path] of the project, relative to its root. */
  fun file(path: String): Path = root.resolve(path)

  companion object {
    /** A node with the local id [id] holding a component `test.Slot` that names [asset]. */
    fun node(
      id: Long,
      name: String,
      asset: Guid? = null,
      parent: Long? = null,
      order: OrderKey = OrderKey.FIRST,
    ): SceneNode =
      SceneNode(
        LocalId(id),
        name,
        parent?.let { LocalId(it) },
        order,
        if (asset == null) emptyMap()
        else mapOf("test.Slot" to JsonObject(mapOf("asset" to JsonPrimitive(asset.toString())))),
      )

    /** A GUID that is the same for the same [n], so tests can read their expectations. */
    fun guid(n: Long): Guid = Guid(n, n)
  }
}

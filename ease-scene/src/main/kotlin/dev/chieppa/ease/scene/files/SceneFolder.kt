package dev.chieppa.ease.scene.files

import dev.chieppa.ease.scene.document.SceneDocument
import java.nio.file.Files
import java.nio.file.Path

/**
 * Reads and saves a scene or prefab folder on disk. Saving writes only the files whose bytes differ
 * and deletes the files of nodes that are gone, so saving an unchanged document touches nothing and
 * git sees one changed file per changed node.
 */
public object SceneFolder {
  /** The document in [folder]. Throws [SceneFileException] when a file cannot be read. */
  public fun read(folder: Path): SceneDocument = SceneFiles.decode(files(folder))

  /**
   * Saves [document] into [folder], creating it when needed, and returns the paths inside the
   * folder (written with `/`) that it wrote or deleted.
   */
  public fun write(folder: Path, document: SceneDocument): SceneSave {
    val wanted = SceneFiles.encode(document)
    val existing = files(folder)
    val written = wanted.filter { (path, text) -> existing[path] != text }.keys.toList()
    val deleted = existing.keys.filter { SceneFiles.isNodeFile(it) && it !in wanted }
    written.forEach { path ->
      val file = folder.resolve(path)
      Files.createDirectories(file.parent)
      Files.writeString(file, wanted.getValue(path))
    }
    deleted.forEach { Files.delete(folder.resolve(it)) }
    return SceneSave(written, deleted)
  }

  /** The text of the header and node files in [folder], by path written with `/`. */
  private fun files(folder: Path): Map<String, String> {
    val found = sortedMapOf<String, String>()
    val header = folder.resolve(SceneFormat.HEADER_FILE)
    if (Files.isRegularFile(header)) found[SceneFormat.HEADER_FILE] = Files.readString(header)
    val nodes = folder.resolve(SceneFormat.NODES_FOLDER)
    if (Files.isDirectory(nodes)) {
      Files.list(nodes).use { list ->
        list
          .filter { Files.isRegularFile(it) }
          .forEach { found[SceneFormat.NODES_FOLDER + "/" + it.fileName] = Files.readString(it) }
      }
    }
    return found.filterKeys { it == SceneFormat.HEADER_FILE || SceneFiles.isNodeFile(it) }
  }
}

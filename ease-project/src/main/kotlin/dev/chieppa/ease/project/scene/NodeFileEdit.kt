package dev.chieppa.ease.project.scene

import dev.chieppa.ease.scene.document.SceneDocument
import dev.chieppa.ease.scene.document.SceneNode
import dev.chieppa.ease.scene.files.SceneFiles
import dev.chieppa.ease.scene.files.SceneFormat
import java.nio.file.Files
import java.nio.file.Path

/**
 * Changes one node file of a scene folder in place, leaving its name and every other file alone.
 */
public object NodeFileEdit {
  /**
   * Replaces the node in the file [path] (inside [folder]) by [change] of it, written in canonical
   * form. Returns whether the file's bytes changed. Throws `SceneFileException` when it cannot be
   * read.
   */
  public fun edit(folder: Path, path: String, change: (SceneNode) -> SceneNode): Boolean {
    val header = SceneFiles.header(SceneDocument())
    val file = folder.resolve(path)
    val old = Files.readString(file)
    val node =
      SceneFiles.decode(mapOf(SceneFormat.HEADER_FILE to header, path to old)).nodes.values.single()
    val written = SceneFiles.encode(SceneDocument(nodes = listOf(change(node))))
    val text = written.entries.single { SceneFiles.isNodeFile(it.key) }.value
    if (text == old) return false
    Files.writeString(file, text)
    return true
  }
}

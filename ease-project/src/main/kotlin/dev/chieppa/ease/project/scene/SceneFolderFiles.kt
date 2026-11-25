package dev.chieppa.ease.project.scene

import dev.chieppa.ease.scene.document.SceneDocument
import dev.chieppa.ease.scene.document.SceneNode
import dev.chieppa.ease.scene.files.SceneFileException
import dev.chieppa.ease.scene.files.SceneFiles
import dev.chieppa.ease.scene.files.SceneFormat
import dev.chieppa.ease.scene.node.LocalId
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

/** A file of a scene folder that could not be read, and why. */
public class UnreadableSceneFile(
  /** The file's path inside the scene folder, written with `/`. */
  public val file: String,
  /** What is wrong with it. */
  public val reason: String,
  /** What kind of fault it is. */
  public val fault: SceneFileFault = SceneFileFault.UNREADABLE,
)

/**
 * A scene folder read file by file: every node file that reads, by its path inside the folder, and
 * the files that did not. Unlike `SceneFolder.read`, one bad file does not hide the rest.
 */
public class SceneFolderFiles
private constructor(
  /** The scene-wide settings and the nodes that read (the first file wins for a repeated id). */
  public val document: SceneDocument,
  /** Each node that read, by its file's path inside the folder. */
  public val nodes: Map<String, SceneNode>,
  /** The files that cannot be read, and files that repeat a node id. */
  public val unreadable: List<UnreadableSceneFile>,
) {
  private val files: Map<LocalId, String> = nodes.entries.associate { it.value.id to it.key }

  /** The path inside the folder of the file holding the node [id], or null when none reads. */
  public fun fileOf(id: LocalId): String? = files[id]

  /** Public entry points. */
  public companion object {
    /** Whether [path] names a scene or prefab folder, by its extension. */
    public fun isSceneFolder(path: Path): Boolean {
      val name = path.fileName.toString()
      return name.endsWith("." + SceneFormat.SCENE_EXTENSION) ||
        name.endsWith("." + SceneFormat.PREFAB_EXTENSION)
    }

    /** Reads the scene or prefab folder [folder]. */
    public fun read(folder: Path): SceneFolderFiles {
      val problems = mutableListOf<UnreadableSceneFile>()
      val header = headerOf(folder, problems)
      val nodes = linkedMapOf<String, SceneNode>()
      val firstById = HashMap<LocalId, String>()
      nodeFiles(folder).forEach { (path, text) ->
        val node = nodeOf(header, path, text, problems) ?: return@forEach
        val first = firstById.putIfAbsent(node.id, path)
        if (first == null) nodes[path] = node else problems += repeated(path, first, node)
      }
      val settings = SceneFiles.decode(mapOf(SceneFormat.HEADER_FILE to header)).settings
      return SceneFolderFiles(SceneDocument(settings, nodes.values), nodes, problems)
    }

    private fun repeated(path: String, first: String, node: SceneNode) =
      UnreadableSceneFile(path, "holds node ${node.id}, as $first does", SceneFileFault.REPEATED_ID)

    /** The header file's text, or the header of an empty scene after noting what is wrong. */
    private fun headerOf(folder: Path, problems: MutableList<UnreadableSceneFile>): String {
      val file = folder.resolve(SceneFormat.HEADER_FILE)
      val text = if (Files.isRegularFile(file)) readText(file) else null
      val failure = if (text == null) "missing" else headerFailure(text)
      if (failure != null) problems += UnreadableSceneFile(SceneFormat.HEADER_FILE, failure)
      return if (failure == null) checkNotNull(text) else SceneFiles.header(SceneDocument())
    }

    private fun headerFailure(text: String): String? =
      try {
        SceneFiles.decode(mapOf(SceneFormat.HEADER_FILE to text))
        null
      } catch (failure: SceneFileException) {
        failure.message.orEmpty()
      }

    private fun nodeOf(
      header: String,
      path: String,
      text: String,
      problems: MutableList<UnreadableSceneFile>,
    ): SceneNode? =
      try {
        SceneFiles.decode(mapOf(SceneFormat.HEADER_FILE to header, path to text))
          .nodes
          .values
          .single()
      } catch (failure: SceneFileException) {
        problems += UnreadableSceneFile(path, failure.message.orEmpty())
        null
      }

    private fun nodeFiles(folder: Path): List<Pair<String, String>> {
      val nodes = folder.resolve(SceneFormat.NODES_FOLDER)
      if (!Files.isDirectory(nodes)) return emptyList()
      return Files.list(nodes)
        .use { list ->
          list
            .filter { Files.isRegularFile(it) }
            .map { SceneFormat.NODES_FOLDER + "/" + it.fileName }
            .filter(SceneFiles::isNodeFile)
            .sorted()
            .toList()
        }
        .mapNotNull { path -> readText(folder.resolve(path))?.let { path to it } }
    }

    private fun readText(file: Path): String? =
      try {
        Files.readString(file)
      } catch (_: IOException) {
        null
      }
  }
}

package dev.chieppa.ease.project.check

import dev.chieppa.ease.assets.Guid
import dev.chieppa.ease.assets.database.AssetDatabase
import dev.chieppa.ease.assets.database.AssetRecord
import dev.chieppa.ease.project.layout.ProjectLayout
import dev.chieppa.ease.project.scene.GuidScan
import dev.chieppa.ease.project.scene.SceneFolderFiles
import dev.chieppa.ease.project.scene.SceneStructure
import dev.chieppa.ease.scene.node.LocalId
import dev.chieppa.ease.scene.resolve.SceneProblem
import dev.chieppa.ease.scene.resolve.SceneResolver
import dev.chieppa.ease.scene.resolve.SceneSource
import dev.chieppa.ease.scene.types.SceneTypes
import java.nio.file.Path

/**
 * The rules over every scene and prefab folder: files that read, one id per node, parents that
 * exist and do not loop, placements that do not contain themselves, sibling order keys, and every
 * asset a node refers to. A reference to a GUID in [pending], one a conflicted sidecar still names,
 * is not reported: the conflict is.
 */
internal class SceneRules(
  private val project: Path,
  private val database: AssetDatabase,
  private val conflicted: Set<Path>,
  private val pending: Set<Guid>,
  private val history: GitHistory?,
) {
  private val seen = LinkedHashSet<String>()
  private val result = mutableListOf<Finding>()
  private val folders = HashMap<Path, SceneFolderFiles>()

  /** A scene folder being inspected, with the way to name its files in reports. */
  private inner class Scene(val record: AssetRecord, val files: SceneFolderFiles) {
    val folder: Path = database.root.resolve(record.path)

    fun display(inside: String): String = ProjectLayout.display(project, folder.resolve(inside))

    /** The report path of the node [id] when it is a node of this scene itself. */
    fun localFile(depth: Int, id: LocalId): String? =
      if (depth == 1) files.fileOf(id)?.let(::display) else null
  }

  fun findings(): List<Finding> {
    database.assets
      .filter { isScene(it.path) }
      .sortedBy { it.path.toString() }
      .forEach { inspect(Scene(it, read(it.path))) }
    return result
  }

  private fun isScene(path: Path): Boolean = SceneFolderFiles.isSceneFolder(path)

  private fun read(path: Path): SceneFolderFiles =
    folders.getOrPut(path) { SceneFolderFiles.read(database.root.resolve(path)) }

  private fun inspect(scene: Scene) {
    scene.files.unreadable.forEach { unreadable ->
      if (scene.folder.resolve(unreadable.file) !in conflicted) {
        val kind =
          if (unreadable.reason.contains(REPEATED_ID)) FindingKind.DUPLICATE_NODE_ID
          else FindingKind.UNREADABLE_SCENE_FILE
        add(Severity.ERROR, kind, scene.display(unreadable.file), unreadable.reason)
      }
    }
    structure(scene)
    resolution(scene)
    references(scene)
  }

  private fun structure(scene: Scene) {
    val document = scene.files.document
    SceneStructure.parentCycles(document).forEach { loop ->
      val names = loop.joinToString { document[it]?.name.orEmpty().ifEmpty { it.text } }
      add(
        Severity.ERROR,
        FindingKind.PARENT_CYCLE,
        scene.display(checkNotNull(scene.files.fileOf(loop.first()))),
        "is in a loop of parents ($names); give one of them another parent",
      )
    }
    SceneStructure.equalOrderKeys(document).forEach { siblings ->
      val files = siblings.map { checkNotNull(scene.files.fileOf(it.id)) }.sorted()
      add(
        Severity.WARNING,
        FindingKind.EQUAL_ORDER_KEYS,
        scene.display(files.first()),
        "${siblings.size} siblings share order key ${siblings.first().order} " +
          "(${files.joinToString { it.substringAfterLast('/') }}); " +
          "ease check --fix gives them keys of their own",
      )
    }
  }

  private fun resolution(scene: Scene) {
    val source = SceneSource { guid ->
      database[guid]?.path?.takeIf(::isScene)?.let { read(it).document }
    }
    val resolver = SceneResolver(SceneTypes.build(), source) { database[it] != null }
    resolver.resolve(scene.record.guid).problems.forEach { problem ->
      when (problem) {
        is SceneProblem.MissingParent ->
          scene.localFile(problem.node.depth, problem.node.id)?.let {
            val message = "names a parent that is not in this scene; give it another parent"
            add(Severity.ERROR, FindingKind.MISSING_PARENT, it, message)
          }
        is SceneProblem.Cycle ->
          scene.localFile(1, problem.node.idAt(0))?.let {
            val message = "places ${problem.asset}, which places this scene again"
            add(Severity.ERROR, FindingKind.PLACEMENT_CYCLE, it, message)
          }
        is SceneProblem.StaleOverride ->
          scene.localFile(problem.instance.depth, problem.instance.id)?.let {
            val message = "overrides ${problem.path}, which the placed asset does not have"
            add(Severity.WARNING, FindingKind.STALE_OVERRIDE, it, message)
          }
        else -> Unit
      }
    }
  }

  private fun references(scene: Scene) {
    scene.files.nodes.forEach { (path, node) ->
      GuidScan.referencesIn(node)
        .filter { database[it] == null && it !in pending }
        .forEach { guid ->
          add(Severity.ERROR, FindingKind.DANGLING_REFERENCE, scene.display(path), dangling(guid))
        }
    }
  }

  private fun dangling(guid: Guid): String {
    val sighting = history?.sighting(guid)
    val where =
      when {
        sighting == null -> "no git history of it was found"
        sighting.deleted ->
          "${sighting.meta} was deleted in ${sighting.shortCommit} by ${sighting.author}; " +
            "ease check --fix restores it"
        else -> "${sighting.meta} had it in ${sighting.shortCommit} by ${sighting.author}"
      }
    return "refers to asset $guid, which the project does not have ($where)"
  }

  private fun add(severity: Severity, kind: FindingKind, file: String, message: String) {
    if (seen.add("$kind|$file|$message")) result += Finding(severity, kind, file, message)
  }

  private companion object {
    const val REPEATED_ID = " as "
  }
}

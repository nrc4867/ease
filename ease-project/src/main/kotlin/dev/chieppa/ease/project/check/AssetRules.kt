package dev.chieppa.ease.project.check

import dev.chieppa.ease.assets.associations.FileAssociationRegistry
import dev.chieppa.ease.assets.database.AssetDatabase
import dev.chieppa.ease.assets.database.AssetProblem
import dev.chieppa.ease.assets.meta.MetaFiles
import dev.chieppa.ease.platform.files.ManualFileWatcher
import dev.chieppa.ease.project.layout.ProjectLayout
import dev.chieppa.ease.scene.assets.registerScenes
import java.nio.file.Path

/**
 * The asset database's own rules over the project's content folder: every sidecar has a source and
 * the other way round, no GUID twice, every file readable. A file that holds conflict markers is
 * left to [ConflictMarkerRule], which names it.
 */
internal class AssetRules(private val project: Path, private val conflicted: Set<Path>) {
  /** The database of the content folder, with every sound asset in it. */
  val database: AssetDatabase =
    AssetDatabase.open(
      ProjectLayout.content(project),
      ManualFileWatcher(),
      associations = FileAssociationRegistry().also { it.registerScenes() },
    )

  fun findings(): List<Finding> = database.problems.mapNotNull(::finding)

  private fun finding(problem: AssetProblem): Finding? =
    when (problem) {
      is AssetProblem.DuplicateGuid -> {
        val first = display(MetaFiles.metaOf(problem.sources.first()))
        error(
          FindingKind.DUPLICATE_GUID,
          MetaFiles.metaOf(problem.sources.last()),
          "uses GUID ${problem.guid}, which $first uses too; " +
            "give one of them a new GUID, and point references at the right one",
        )
      }
      is AssetProblem.MetaWithoutSource ->
        error(
          FindingKind.ORPHAN_SIDECAR,
          problem.meta,
          "has no source file; delete the sidecar, or restore the file",
        )
      is AssetProblem.SourceWithoutMeta ->
        error(
          FindingKind.MISSING_SIDECAR,
          problem.source,
          "has no .meta sidecar; import it in the editor to give it a GUID",
        )
      is AssetProblem.UnreadableMeta ->
        error(FindingKind.UNREADABLE_ASSET, problem.meta, problem.reason, skipIfConflicted = true)
      is AssetProblem.UnreadableSource ->
        error(FindingKind.UNREADABLE_ASSET, problem.source, problem.reason)
    }

  private fun error(
    kind: FindingKind,
    file: Path,
    message: String,
    skipIfConflicted: Boolean = false,
  ): Finding? {
    val absolute = ProjectLayout.content(project).resolve(file)
    if (skipIfConflicted && absolute in conflicted) return null
    return Finding(Severity.ERROR, kind, ProjectLayout.display(project, absolute), message)
  }

  private fun display(file: Path): String =
    ProjectLayout.display(project, ProjectLayout.content(project).resolve(file))
}

package dev.chieppa.ease.project.check

import dev.chieppa.ease.project.layout.ProjectLayout
import java.nio.file.Path

/** No conflict markers remain in any text file. */
internal class ConflictMarkerRule(private val project: Path) {
  /** The files holding conflict markers, relative to the project. */
  val conflicted: List<Path> =
    ProjectFiles.all(project).filter { file ->
      ProjectFiles.textOf(file)?.let(ConflictMarkers::isConflicted) == true
    }

  fun findings(): List<Finding> = conflicted.map {
    Finding(
      Severity.ERROR,
      FindingKind.CONFLICT_MARKERS,
      ProjectLayout.display(project, it),
      "holds conflict markers (<<<<<<<, =======, >>>>>>>); settle them and stage the file",
    )
  }
}

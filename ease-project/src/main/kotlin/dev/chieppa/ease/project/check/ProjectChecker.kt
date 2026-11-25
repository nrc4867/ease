package dev.chieppa.ease.project.check

import dev.chieppa.ease.assets.Guid
import dev.chieppa.ease.project.git.Git
import dev.chieppa.ease.project.layout.ProjectLayout
import dev.chieppa.ease.project.scene.GuidScan
import java.nio.file.Files
import java.nio.file.Path

/**
 * `ease check`: the rules that catch a bad merge in the project folder [project]. Sidecars pair
 * with their sources, no GUID is twice, scenes read and their nodes have parents and order keys,
 * every reference resolves, no conflict markers remain, and binary assets are in Git LFS. The asset
 * database and the scene resolver supply their own problems; the rest is checked here.
 */
public class ProjectChecker(private val project: Path, private val git: Git = Git(project)) {
  /** Runs every rule and returns what is wrong. */
  public fun check(): CheckReport {
    val markers = ConflictMarkerRule(project)
    val findings = markers.findings().toMutableList()
    if (Files.isDirectory(ProjectLayout.content(project))) {
      val conflicted = markers.conflicted.toSet()
      val assets = AssetRules(project, conflicted)
      findings += assets.findings()
      val history = if (ProjectLayout.isRepository(project)) GitHistory(git) else null
      findings +=
        SceneRules(project, assets.database, conflicted, pendingGuids(conflicted), history)
          .findings()
    }
    findings += LfsRule(project, git).findings()
    return CheckReport(findings)
  }

  /** The GUIDs that conflicted sidecars name, whichever side holds them. */
  private fun pendingGuids(conflicted: Set<Path>): Set<Guid> =
    conflicted
      .filter { it.fileName.toString().endsWith(".meta") }
      .flatMap { GuidScan.guidsIn(Files.readString(it)) }
      .toSet()
}

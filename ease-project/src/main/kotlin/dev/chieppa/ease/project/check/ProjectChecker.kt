package dev.chieppa.ease.project.check

import dev.chieppa.ease.assets.Guid
import dev.chieppa.ease.assets.meta.MetaFiles
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
 *
 * With [staged] the rules read the files in git's index, as a pre-commit hook needs, so untracked
 * and unstaged files neither block a commit nor hide a bad staged version.
 */
public class ProjectChecker(
  private val project: Path,
  private val git: Git = Git(project),
  private val staged: Boolean = false,
) {
  /** Runs every rule and returns what is wrong. */
  public fun check(): CheckReport {
    val history = if (ProjectLayout.isRepository(project)) GitHistory(git) else null
    val content =
      if (staged && history != null) {
        StagedTree.of(git).use { contentFindings(it.root, history) }
      } else {
        contentFindings(project, history)
      }
    return CheckReport(content + LfsRule(project, git, staged).findings())
  }

  /** The findings that need only the files of [root], laid out as the project is. */
  private fun contentFindings(root: Path, history: GitHistory?): List<Finding> {
    val markers = ConflictMarkerRule(root)
    val findings = markers.findings().toMutableList()
    if (Files.isDirectory(ProjectLayout.content(root))) {
      val conflicted = markers.conflicted.toSet()
      val assets = AssetRules(root, conflicted)
      findings += assets.findings()
      findings +=
        SceneRules(root, assets.database, conflicted, pendingGuids(conflicted), history).findings()
    }
    return findings
  }

  /** The GUIDs that conflicted sidecars name, whichever side holds them. */
  private fun pendingGuids(conflicted: Set<Path>): Set<Guid> =
    conflicted
      .filter { MetaFiles.isMeta(it) }
      .flatMap { GuidScan.guidsIn(Files.readString(it)) }
      .toSet()
}

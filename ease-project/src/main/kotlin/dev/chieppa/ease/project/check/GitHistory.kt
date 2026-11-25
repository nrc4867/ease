package dev.chieppa.ease.project.check

import dev.chieppa.ease.assets.Guid
import dev.chieppa.ease.project.git.Git
import dev.chieppa.ease.project.git.GitFailure

/** Where git's history last had an asset: its sidecar, the commit and who made it. */
public class AssetSighting(
  /** The commit that last added, changed or deleted the sidecar. */
  public val commit: String,
  /** Who made the commit. */
  public val author: String,
  /** The sidecar's path from the repository root. */
  public val meta: String,
  /** Whether that commit deleted it. */
  public val deleted: Boolean,
) {
  /** The commit's first characters, as people quote it. */
  public val shortCommit: String
    get() = commit.take(SHORT)

  private companion object {
    const val SHORT = 8
  }
}

/** Looks an asset's GUID up in git's history, for dangling references. */
public class GitHistory(private val git: Git) {
  private val cache = HashMap<Guid, AssetSighting?>()

  /** The newest commit that touched a sidecar holding [guid], or null when none did. */
  public fun sighting(guid: Guid): AssetSighting? = cache.getOrPut(guid) { search(guid) }

  private fun search(guid: Guid): AssetSighting? =
    try {
      val log =
        git.run(
          "log",
          "--all",
          "--no-renames",
          "-S$guid",
          "--format=$MARK%H%x09%an",
          "--name-status",
          "--",
          "*.meta",
        )
      if (log.succeeded) parse(log.lines, guid) else null
    } catch (_: GitFailure) {
      null
    }

  /** One commit of the log with the sidecars it changed, as git's status letter and path. */
  private class Entry(val commit: String, val author: String) {
    val changes = mutableListOf<Pair<String, String>>()
  }

  private fun parse(lines: List<String>, guid: Guid): AssetSighting? =
    entries(lines).take(MAX_COMMITS).firstNotNullOfOrNull { sighting(it, guid) }

  private fun entries(lines: List<String>): List<Entry> {
    val entries = mutableListOf<Entry>()
    lines.forEach { line ->
      val fields = line.removePrefix(MARK).split(TAB, limit = 2)
      if (line.startsWith(MARK)) {
        entries += Entry(fields[0], fields.getOrElse(1) { "" })
      } else if (fields.size == 2) {
        entries.lastOrNull()?.changes?.add(fields[0] to fields[1])
      }
    }
    return entries
  }

  private fun sighting(entry: Entry, guid: Guid): AssetSighting? =
    entry.changes.firstNotNullOfOrNull { (status, path) ->
      val deleted = status == DELETED
      val revision = if (deleted) "${entry.commit}^" else entry.commit
      AssetSighting(entry.commit, entry.author, path, deleted).takeIf {
        holds(revision, path, guid)
      }
    }

  private fun holds(revision: String, path: String, guid: Guid): Boolean =
    git.run("show", "$revision:$path").let { it.succeeded && it.output.contains(guid.toString()) }

  private companion object {
    const val MARK = "commit "
    const val MAX_COMMITS = 40
    const val TAB = '\t'
    const val DELETED = "D"
  }
}

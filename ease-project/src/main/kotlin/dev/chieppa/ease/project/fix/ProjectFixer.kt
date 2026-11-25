package dev.chieppa.ease.project.fix

import dev.chieppa.ease.assets.Guid
import dev.chieppa.ease.assets.database.AssetDatabase
import dev.chieppa.ease.assets.meta.AssetMeta
import dev.chieppa.ease.assets.meta.MetaFiles
import dev.chieppa.ease.project.check.AssetRules
import dev.chieppa.ease.project.check.ConflictMarkerRule
import dev.chieppa.ease.project.check.ConflictMarkers
import dev.chieppa.ease.project.check.GitHistory
import dev.chieppa.ease.project.git.Git
import dev.chieppa.ease.project.git.GitFailure
import dev.chieppa.ease.project.layout.ProjectLayout
import dev.chieppa.ease.project.scene.NodeFileEdit
import dev.chieppa.ease.project.scene.SceneFolderFiles
import dev.chieppa.ease.project.scene.SceneStructure
import dev.chieppa.ease.serialization.yaml.CanonicalYaml
import dev.chieppa.ease.serialization.yaml.YamlFormatException
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.serialization.SerializationException

/**
 * The mechanical fixes `ease check --fix` makes, none of which picks a side of a real disagreement:
 *
 * - A sidecar conflicted because two branches imported the same file keeps our GUID, and every
 *   reference to theirs is rewritten to it.
 * - A reference to an asset that is gone is pointed at the sidecar that now holds the GUID's path,
 *   or the deleted asset is restored from git's history.
 * - Siblings with equal order keys get keys of their own.
 *
 * Whatever else is wrong is left for a person; run the check again to see it.
 */
public class ProjectFixer(private val project: Path, private val git: Git = Git(project)) {
  private val references = ReferenceRewriter(project)
  private val repository = ProjectLayout.isRepository(project)

  /** Makes the fixes and says what it changed. */
  public fun fix(): FixReport {
    val changes = mutableListOf<String>()
    changes += settleSidecarConflicts()
    changes += repairDanglingReferences()
    changes += respaceOrderKeys()
    return FixReport(changes)
  }

  private fun settleSidecarConflicts(): List<String> {
    val changes = mutableListOf<String>()
    val mapping = LinkedHashMap<Guid, Guid>()
    ConflictMarkerRule(project)
      .conflicted
      .filter { MetaFiles.isMeta(it) }
      .forEach { meta ->
        val text = Files.readString(meta)
        val ours = metaOf(ConflictMarkers.ours(text)) ?: return@forEach
        val theirs = metaOf(ConflictMarkers.theirs(text)) ?: return@forEach
        MetaFiles.write(meta, ours)
        stage(meta)
        val name = ProjectLayout.display(project, meta)
        if (ours.guid == theirs.guid) {
          changes += "$name: settled a conflict with no difference"
        } else {
          mapping[theirs.guid] = ours.guid
          changes += "$name: kept GUID ${ours.guid}, dropped ${theirs.guid}"
        }
      }
    if (mapping.isNotEmpty()) changes += pointed(references.rewrite(mapping), mapping)
    return changes
  }

  private fun repairDanglingReferences(): List<String> {
    val database = AssetRules(project, emptySet()).database
    val missing = references.references().filter { database[it] == null }
    val history = if (repository) GitHistory(git) else null
    val changes = mutableListOf<String>()
    val repoint = LinkedHashMap<Guid, Guid>()
    missing.forEach { guid ->
      val sighting = history?.sighting(guid) ?: return@forEach
      val current = currentGuid(database, sighting.meta)
      when {
        sighting.deleted -> restore(sighting.commit, sighting.meta)?.let { changes += it }
        current != null && current != guid -> repoint[guid] = current
      }
    }
    if (repoint.isNotEmpty()) changes += pointed(references.rewrite(repoint), repoint)
    return changes
  }

  /** The GUID of the asset whose sidecar is at [meta] now, or null when there is none. */
  private fun currentGuid(database: AssetDatabase, meta: String): Guid? =
    database
      .at(MetaFiles.sourceOf(ProjectLayout.content(project).relativize(project.resolve(meta))))
      ?.guid

  private fun restore(commit: String, meta: String): String? {
    val before = "$commit^"
    val source = meta.removeSuffix(MetaFiles.SUFFIX)
    val paths = listOf(meta) + git.run("ls-tree", "-r", "--name-only", before, "--", source).lines
    val result = git.run(listOf("checkout", before, "--") + paths)
    return if (result.succeeded) "restored ${paths.size} file(s) of $source from $before" else null
  }

  private fun respaceOrderKeys(): List<String> =
    references.folders().flatMap { folder ->
      val scene = SceneFolderFiles.read(folder)
      val keys = SceneStructure.respaced(scene.document)
      keys.mapNotNull { (id, key) ->
        val file = scene.fileOf(id) ?: return@mapNotNull null
        NodeFileEdit.edit(folder, file) { it.copy(order = key) }
        "${ProjectLayout.display(project, folder.resolve(file))}: order key set to $key"
      }
    }

  private fun pointed(files: List<String>, mapping: Map<Guid, Guid>): List<String> =
    files.map { file ->
      val pairs = mapping.entries.joinToString { "${it.key} to ${it.value}" }
      "$file: pointed references ($pairs)"
    }

  private fun metaOf(text: String?): AssetMeta? = text?.let {
    try {
      CanonicalYaml.decode(AssetMeta.serializer(), it + "\n")
    } catch (_: SerializationException) {
      null
    } catch (_: YamlFormatException) {
      null
    }
  }

  private fun stage(file: Path) {
    if (!repository) return
    try {
      git.run("add", "--", ProjectLayout.display(project, file))
    } catch (_: GitFailure) {
      return
    }
  }
}

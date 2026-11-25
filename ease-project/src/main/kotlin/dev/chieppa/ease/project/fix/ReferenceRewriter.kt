package dev.chieppa.ease.project.fix

import dev.chieppa.ease.assets.Guid
import dev.chieppa.ease.project.layout.ProjectLayout
import dev.chieppa.ease.project.scene.GuidScan
import dev.chieppa.ease.project.scene.NodeFileEdit
import dev.chieppa.ease.project.scene.SceneFolderFiles
import java.nio.file.Files
import java.nio.file.Path

/** Reads and rewrites the asset references in every scene and prefab folder of a project. */
internal class ReferenceRewriter(private val project: Path) {
  private val content = ProjectLayout.content(project)

  /** Every scene and prefab folder under the content folder. */
  fun folders(): List<Path> =
    if (!Files.isDirectory(content)) {
      emptyList()
    } else {
      Files.walk(content).use { walk ->
        walk
          .filter { Files.isDirectory(it) && SceneFolderFiles.isSceneFolder(it) }
          .sorted()
          .toList()
      }
    }

  /** Every GUID a node of any scene refers to. */
  fun references(): Set<Guid> =
    folders()
      .flatMap { SceneFolderFiles.read(it).nodes.values }
      .flatMap(GuidScan::referencesIn)
      .toSet()

  /**
   * Replaces each reference found in [mapping] by what it maps to, in every node file that holds
   * one. Returns the project paths of the files it wrote.
   */
  fun rewrite(mapping: Map<Guid, Guid>): List<String> =
    folders().flatMap { folder ->
      SceneFolderFiles.read(folder)
        .nodes
        .filterValues { node -> GuidScan.referencesIn(node).any { it in mapping } }
        .keys
        .filter { NodeFileEdit.edit(folder, it) { node -> GuidScan.rewrite(node, mapping) } }
        .map { ProjectLayout.display(project, folder.resolve(it)) }
    }
}

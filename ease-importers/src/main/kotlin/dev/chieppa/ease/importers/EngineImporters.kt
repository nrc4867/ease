package dev.chieppa.ease.importers

import dev.chieppa.ease.assets.associations.ContributorProvider
import dev.chieppa.ease.assets.associations.FileAssociations
import dev.chieppa.ease.assets.associations.FileMatch
import dev.chieppa.ease.assets.importing.Importer
import dev.chieppa.ease.importers.gltf.GltfImporter

/** Where the engine's importers say they came from. */
private const val ORIGIN = "ease-importers"

/**
 * The engine's own importers, registered like any game's: the glTF importer for `.gltf` and `.glb`.
 * `ease import`, the editor and a game's content find it through the service loader.
 */
public class EngineImporters : ContributorProvider {
  override fun registerContributors(associations: FileAssociations) {
    val gltf = GltfImporter()
    for (extension in listOf("gltf", "glb")) {
      associations.register(Importer.kind, FileMatch.Extension(extension), 0, gltf, ORIGIN)
    }
  }
}

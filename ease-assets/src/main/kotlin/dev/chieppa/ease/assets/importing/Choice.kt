package dev.chieppa.ease.assets.importing

import dev.chieppa.ease.assets.associations.AmbiguousContributors
import dev.chieppa.ease.assets.associations.FileAssociations
import dev.chieppa.ease.assets.associations.ProjectFile
import dev.chieppa.ease.assets.database.AssetDatabase
import dev.chieppa.ease.assets.database.AssetRecord
import dev.chieppa.ease.assets.meta.MetaFileException
import dev.chieppa.ease.assets.meta.MetaFiles
import dev.chieppa.ease.assets.presets.FolderPresets
import dev.chieppa.ease.assets.presets.PresetFileException
import kotlinx.serialization.json.JsonObject

/** Thrown when no importer can be chosen for a source; the message says why. */
internal class ChoiceFailure(message: String, cause: Throwable? = null) :
  RuntimeException(message, cause)

/** The importer and settings one asset imports with, from its sidecar, presets and file type. */
internal class Choice(val importer: Importer?, val settings: JsonObject) {
  companion object {
    /**
     * The choice for [record]: the sidecar's importer, else the nearest preset's, else the one the
     * file's type matches; settings are the presets' with the sidecar's on top.
     */
    fun of(database: AssetDatabase, associations: FileAssociations, record: AssetRecord): Choice =
      try {
        choose(database, associations, record)
      } catch (failure: MetaFileException) {
        throw ChoiceFailure(failure.message.orEmpty(), failure)
      } catch (failure: PresetFileException) {
        throw ChoiceFailure(failure.message.orEmpty(), failure)
      } catch (failure: AmbiguousContributors) {
        throw ChoiceFailure(failure.message.orEmpty(), failure)
      }

    private fun choose(
      database: AssetDatabase,
      associations: FileAssociations,
      record: AssetRecord,
    ): Choice {
      val meta = MetaFiles.read(MetaFiles.metaOf(database.root.resolve(record.path)))
      val presets = FolderPresets.choiceFor(database.root, record.path)
      val settings = JsonObject(presets.settings + meta.settings.orEmpty())
      val named = meta.importer ?: presets.importer
      val importer =
        if (named != null) {
          associations.contributors(Importer.kind).firstOrNull { it.id == named }
            ?: throw ChoiceFailure("names the importer '$named', which no loaded module registers")
        } else {
          associations
            .resolve(Importer.kind, ProjectFile.under(database.root, record.path))
            .firstOrNull()
        }
      return Choice(importer, settings)
    }
  }
}

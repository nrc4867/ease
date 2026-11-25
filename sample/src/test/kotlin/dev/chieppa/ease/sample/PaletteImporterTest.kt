package dev.chieppa.ease.sample

import dev.chieppa.ease.assets.associations.FileAssociationRegistry
import dev.chieppa.ease.assets.associations.ModuleRegistrations
import dev.chieppa.ease.assets.database.AssetDatabase
import dev.chieppa.ease.assets.importing.ImportException
import dev.chieppa.ease.assets.importing.ImportService
import dev.chieppa.ease.platform.files.ManualFileWatcher
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.io.TempDir

class PaletteImporterTest {
  @TempDir lateinit var folder: Path

  @Test
  fun `colors cook to a count and rgb bytes, with their names`() {
    val cooked =
      PaletteImporter.import("b 000102\na ff0000\n".toByteArray(), JsonObject(emptyMap()))

    assertContentEquals(
      byteArrayOf(2, 0, 1, 2, -1, 0, 0),
      cooked.single { it.name == "colors.bin" }.bytes,
    )
    assertEquals("b\na\n", cooked.single { it.name == "names.txt" }.bytes.decodeToString())
  }

  @Test
  fun `the sorted setting orders colors by name`() {
    val settings = JsonObject(mapOf("sorted" to JsonPrimitive(true)))

    val cooked = PaletteImporter.import("b 000000\na ffffff\n".toByteArray(), settings)

    assertEquals("a\nb\n", cooked.single { it.name == "names.txt" }.bytes.decodeToString())
  }

  @Test
  fun `a bad line names its number`() {
    val failure =
      assertFailsWith<ImportException> {
        PaletteImporter.import("ok 000000\nbad\n".toByteArray(), JsonObject(emptyMap()))
      }

    assertEquals("Line 2: expected 'name rrggbb', got 'bad'", failure.message)
  }

  @Test
  fun `the game's module registers the importer, and unloading it removes the results`() {
    val content = SampleContent(folder).folder
    val associations = FileAssociationRegistry()
    val module = ModuleRegistrations(associations)
    SampleGame().registerContributors(module)
    val database = AssetDatabase.open(content, ManualFileWatcher(), associations = associations)
    ImportService(database, associations).use { service ->
      val report = service.refresh()
      val palette = checkNotNull(database.at(Path.of("trail.palette")))

      assertEquals(listOf(Path.of("trail.palette")), report.imported)
      assertEquals(listOf("colors.bin", "names.txt"), service.cooked(palette.guid).map { it.name })

      module.close()

      assertEquals(emptyList(), service.cooked(palette.guid))
    }
  }
}

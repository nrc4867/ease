package dev.chieppa.ease.assets.importing

import dev.chieppa.ease.assets.Guid
import dev.chieppa.ease.assets.TestProject
import dev.chieppa.ease.assets.associations.FileAssociationRegistry
import dev.chieppa.ease.assets.associations.FileMatch
import dev.chieppa.ease.assets.associations.ModuleRegistrations
import dev.chieppa.ease.assets.database.AssetDatabase
import dev.chieppa.ease.assets.meta.AssetMeta
import dev.chieppa.ease.assets.meta.MetaFiles
import dev.chieppa.ease.assets.presets.FolderPreset
import dev.chieppa.ease.assets.presets.FolderPresets
import dev.chieppa.ease.platform.files.ManualFileWatcher
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.io.TempDir

class ImportServiceTest {
  @TempDir lateinit var folder: Path

  private val project by lazy { TestProject(folder) }
  private val associations = FileAssociationRegistry()

  /** Cooks `out.txt`: the source text, upper-cased when the setting `shout` is true. */
  private class Echo(override val id: String, override val version: Int = 1) : Importer {
    var runs = 0

    override fun import(source: ByteArray, settings: JsonObject): List<CookedAsset> {
      runs++
      val text = source.toString(Charsets.UTF_8)
      if (text == "broken") throw ImportException("the source is broken")
      val shout = (settings["shout"] as? JsonPrimitive)?.content == "true"
      return listOf(CookedAsset("out.txt", (if (shout) text.uppercase() else text).toByteArray()))
    }
  }

  private fun service(): ImportService {
    val database = AssetDatabase.open(folder, ManualFileWatcher(), project.guids, associations)
    return ImportService(database, associations)
  }

  private fun cookedText(service: ImportService, guid: Guid): String {
    val cooked = service.cooked(guid).single()
    return cooked.bytes.toString(Charsets.UTF_8)
  }

  private fun choose(
    name: String,
    guid: Guid,
    importer: String? = null,
    settings: JsonObject? = null,
  ) =
    MetaFiles.write(
      MetaFiles.metaOf(project.path(name)),
      AssetMeta(guid = guid, importer = importer, settings = settings),
    )

  @Test
  fun `a source imports with the importer its extension matches, once per change`() {
    val echo = Echo("echo")
    associations.register(Importer.kind, FileMatch.Extension("txt"), 0, echo)
    val guid = project.asset("a.txt", "hello")
    service().use { service ->
      assertEquals(listOf(Path.of("a.txt")), service.refresh().imported)
      assertEquals("hello", cookedText(service, guid))

      val again = service.refresh()

      assertEquals(emptyList(), again.imported)
      assertEquals(1, again.upToDate)
      assertEquals(1, echo.runs)
    }
  }

  @Test
  fun `settings come from presets and the sidecar, the sidecar winning`() {
    associations.register(Importer.kind, FileMatch.Extension("txt"), 0, Echo("echo"))
    project.write(
      "loud/${FolderPresets.FILE_NAME}",
      FolderPresets.encode(
        FolderPreset(settings = JsonObject(mapOf("shout" to JsonPrimitive(true))))
      ),
    )
    val plain = project.asset("quiet.txt", "abc")
    val loud = project.asset("loud/a.txt", "abc")
    val override = project.asset("loud/b.txt", "abc")
    choose("loud/b.txt", override, settings = JsonObject(mapOf("shout" to JsonPrimitive(false))))
    service().use { service ->
      service.refresh()

      assertEquals("abc", cookedText(service, plain))
      assertEquals("ABC", cookedText(service, loud))
      assertEquals("abc", cookedText(service, override))
    }
  }

  @Test
  fun `a sidecar names an importer other than the matching one`() {
    associations.register(Importer.kind, FileMatch.Extension("txt"), 0, Echo("echo"))
    associations.register(Importer.kind, FileMatch.Extension("bin"), 0, Echo("other"))
    choose("a.txt", project.asset("a.txt", "x"), importer = "other")
    choose("b.txt", project.asset("b.txt", "x"), importer = "nope")
    service().use { service ->
      val report = service.refresh()

      assertEquals(listOf(Path.of("a.txt")), report.imported)
      assertEquals(listOf(Path.of("b.txt")), report.problems.map { it.source })
      val problem = report.problems.single()
      assertTrue(problem.message.contains("nope"))
    }
  }

  @Test
  fun `a preset names the importer for everything under its folder`() {
    associations.register(Importer.kind, FileMatch.Extension("txt"), 0, Echo("echo"))
    associations.register(Importer.kind, FileMatch.Extension("bin"), 0, Echo("other"))
    project.write(
      "deep/${FolderPresets.FILE_NAME}",
      FolderPresets.encode(FolderPreset(importer = "other")),
    )
    project.asset("deep/down/c.dat", "x")
    service().use { service ->
      assertEquals(listOf(Path.of("deep/down/c.dat")), service.refresh().imported)
    }
  }

  @Test
  fun `two importers tied on one file report an error and import nothing`() {
    associations.register(Importer.kind, FileMatch.Extension("txt"), 0, Echo("one"), "module one")
    associations.register(Importer.kind, FileMatch.Extension("txt"), 0, Echo("two"), "module two")
    project.asset("a.txt", "x")
    service().use { service ->
      val report = service.refresh()

      assertEquals(emptyList(), report.imported)
      val message = report.problems.single().message
      assertTrue(message.contains("module one") && message.contains("module two"), message)
    }
  }

  @Test
  fun `a content sniffer separates formats that share an extension`() {
    associations.register(Importer.kind, FileMatch.Extension("json"), 0, Echo("plain"))
    associations.register(
      Importer.kind,
      FileMatch.Sniffer.startingWith("json", "{\"kind\":\"rock\""),
      0,
      Echo("rock", version = 7),
    )
    val rock = project.asset("a.json", "{\"kind\":\"rock\"}")
    val other = project.asset("b.json", "{}")
    service().use { service ->
      service.refresh()

      assertEquals("{\"kind\":\"rock\"}", cookedText(service, rock))
      assertEquals("{}", cookedText(service, other))
      assertEquals(emptyList(), service.problems)
    }
  }

  @Test
  fun `a failed import keeps the last good result and reports a problem`() {
    associations.register(Importer.kind, FileMatch.Extension("txt"), 0, Echo("echo"))
    val guid = project.asset("a.txt", "good")
    service().use { it.refresh() }
    project.write("a.txt", "broken")

    service().use { service ->
      val report = service.refresh()

      assertEquals("a.txt: echo failed: the source is broken", report.problems.single().toString())
      assertEquals("good", cookedText(service, guid))
    }
  }

  @Test
  fun `closing an importer's registration removes its results, and registering brings them back`() {
    val module = ModuleRegistrations(associations)
    module.register(Importer.kind, FileMatch.Extension("txt"), 0, Echo("echo"))
    val guid = project.asset("a.txt", "x")
    service().use { service ->
      service.refresh()
      assertEquals(1, service.cooked(guid).size)

      module.close()

      assertEquals(emptyList(), service.cooked(guid))
      associations.register(Importer.kind, FileMatch.Extension("txt"), 0, Echo("echo"))
      assertEquals(1, service.cooked(guid).size)
    }
  }

  @Test
  fun `a new importer version imports again`() {
    val first = associations.register(Importer.kind, FileMatch.Extension("txt"), 0, Echo("echo"))
    project.asset("a.txt", "x")
    service().use { it.refresh() }
    first.close()
    associations.register(Importer.kind, FileMatch.Extension("txt"), 0, Echo("echo", version = 2))

    val report = service().use { it.refresh() }

    assertEquals(listOf(Path.of("a.txt")), report.imported)
  }

  @Test
  fun `two importers with one id fail a sidecar that names it, naming both origins`() {
    associations.register(Importer.kind, FileMatch.Extension("txt"), 0, Echo("same"), "module one")
    associations.register(Importer.kind, FileMatch.Extension("bin"), 0, Echo("same"), "module two")
    choose("a.txt", project.asset("a.txt", "x"), importer = "same")
    service().use { service ->
      val problem = service.refresh().problems.single()
      val message = problem.message

      assertTrue(message.contains("module one") && message.contains("module two"), message)
    }
  }

  @Test
  fun `a stray file in the import cache does not stop a refresh`() {
    associations.register(Importer.kind, FileMatch.Extension("txt"), 0, Echo("echo"))
    val guid = project.asset("a.txt", "x")
    service().use { it.refresh() }
    project.write(".ease/cache/imports/.DS_Store", "junk")

    service().use { service ->
      assertEquals(emptyList(), service.refresh().problems)
      assertEquals(1, service.cooked(guid).size)
    }
  }

  @Test
  fun `an unreadable meta file keeps the last good result and reports a problem`() {
    associations.register(Importer.kind, FileMatch.Extension("txt"), 0, Echo("echo"))
    val guid = project.asset("a.txt", "x")
    service().use { it.refresh() }
    project.write("a.txt.meta", "formatVersion: 2\nguid: not-a-guid\n")

    service().use { service ->
      val report = service.refresh()

      assertEquals(listOf(Path.of("a.txt")), report.problems.map { it.source })
      assertEquals(1, service.cooked(guid).size)
    }
  }

  @Test
  fun `closing a module's registrations also removes the listeners it made`() {
    val module = ModuleRegistrations(associations)
    var calls = 0
    module.onChange { calls++ }

    module.close()
    associations.register(Importer.kind, FileMatch.Extension("txt"), 0, Echo("echo"))

    assertEquals(0, calls)
  }

  @Test
  fun `closing a module closes every registration even when one throws`() {
    val module = ModuleRegistrations(associations)
    var armed = false
    module.onChange { if (armed) error("listener failed") }
    val first = module.register(Importer.kind, FileMatch.Extension("txt"), 0, Echo("a"))
    val second = module.register(Importer.kind, FileMatch.Extension("bin"), 0, Echo("b"))
    armed = true

    assertFailsWith<IllegalStateException> { module.close() }

    assertTrue(!first.isOpen && !second.isOpen)
  }
}

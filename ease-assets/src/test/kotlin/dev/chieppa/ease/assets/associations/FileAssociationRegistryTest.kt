package dev.chieppa.ease.assets.associations

import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FileAssociationRegistryTest {
  private val registry = FileAssociationRegistry()

  private interface Importer : Contributor

  private interface Inspector : Contributor

  private class Named(val label: String) : Importer, Inspector

  private val importers = ContributorKind("Importer", Importer::class, Cardinality.ONE)
  private val inspectors = ContributorKind("Inspector", Inspector::class, Cardinality.MANY)

  private fun file(name: String, head: String = "") =
    ProjectFile(Path.of("content", name)) { head.toByteArray() }

  private fun labels(kind: ContributorKind<*>, file: ProjectFile): List<String> =
    when (kind) {
      importers -> registry.resolve(importers, file).map { (it as Named).label }
      else -> registry.resolve(inspectors, file).map { (it as Named).label }
    }

  @Test
  fun `a contributor registers, resolves for its file type, and unregisters`() {
    val registration =
      registry.register(importers, FileMatch.Extension("png"), 0, Named("png importer"))

    assertEquals(listOf("png importer"), labels(importers, file("rock.png")))
    assertEquals(emptyList(), labels(importers, file("rock.jpg")))
    assertTrue(registration.isOpen)

    registration.close()

    assertFalse(registration.isOpen)
    assertEquals(emptyList(), labels(importers, file("rock.png")))
    registration.close()
  }

  @Test
  fun `extensions match without regard to case or a leading dot`() {
    registry.register(importers, FileMatch.Extension(".PNG"), 0, Named("png"))

    assertEquals(listOf("png"), labels(importers, file("Rock.Png")))
  }

  @Test
  fun `a glob is more specific than an extension, and a sniffer more than a glob`() {
    registry.register(inspectors, FileMatch.Extension("json"), 0, Named("json"))
    registry.register(inspectors, FileMatch.Glob("*.geo.json"), 0, Named("geo"))
    registry.register(
      inspectors,
      FileMatch.Sniffer("json") { it.decodeToString().contains("\"species\"") },
      0,
      Named("species"),
    )

    assertEquals(listOf("json"), labels(inspectors, file("a.json", "{}")))
    assertEquals(listOf("geo", "json"), labels(inspectors, file("a.geo.json", "{}")))
    assertEquals(
      listOf("species", "geo", "json"),
      labels(inspectors, file("a.geo.json", "{\"species\": 1}")),
    )
  }

  @Test
  fun `priority orders contributors of the same specificity`() {
    registry.register(importers, FileMatch.Extension("gltf"), 1, Named("low"))
    registry.register(importers, FileMatch.Extension("gltf"), 5, Named("high"))

    assertEquals(listOf("high", "low"), labels(importers, file("m.gltf")))
  }

  @Test
  fun `a glob with a slash matches the path, one without matches the name`() {
    registry.register(inspectors, FileMatch.Glob("characters/*.png"), 0, Named("path"))
    registry.register(inspectors, FileMatch.Glob("hero_*"), 0, Named("name"))

    val inFolder = ProjectFile(Path.of("characters", "hero_a.png")) { ByteArray(0) }

    assertEquals(setOf("path", "name"), labels(inspectors, inFolder).toSet())
    assertEquals(emptyList(), labels(inspectors, file("x.png")))
  }

  @Test
  fun `a tie for a one-per-file kind is an error naming where each contender came from`() {
    registry.register(importers, FileMatch.Extension("png"), 3, Named("a"), origin = "ease-core")
    registry.register(importers, FileMatch.Extension("png"), 3, Named("b"), origin = "game-mod")

    val failure =
      assertFailsWith<AmbiguousContributors> { registry.resolve(importers, file("x.png")) }

    assertEquals(2, failure.contenders.size)
    assertTrue("ease-core" in failure.message.orEmpty() && "game-mod" in failure.message.orEmpty())
    assertTrue("x.png" in failure.message.orEmpty())
  }

  @Test
  fun `a tie is no error for a kind that takes many`() {
    registry.register(inspectors, FileMatch.Extension("png"), 3, Named("a"))
    registry.register(inspectors, FileMatch.Extension("png"), 3, Named("b"))

    assertEquals(listOf("a", "b"), labels(inspectors, file("x.png")))
  }

  @Test
  fun `removing one of two tied contributors ends the tie`() {
    registry.register(importers, FileMatch.Extension("png"), 3, Named("a"))
    val second = registry.register(importers, FileMatch.Extension("png"), 3, Named("b"))

    second.close()

    assertEquals(listOf("a"), labels(importers, file("x.png")))
  }

  @Test
  fun `attachments list every kind with its origin`() {
    registry.register(importers, FileMatch.Extension("png"), 0, Named("i"), origin = "ease")
    registry.register(inspectors, FileMatch.Glob("*.png"), 2, Named("s"), origin = "plugin")

    val attached = registry.attachments(file("x.png"))

    assertEquals(setOf("ease", "plugin"), attached.map { it.origin }.toSet())
    assertEquals(setOf("Importer", "Inspector"), attached.map { it.kind.name }.toSet())
  }

  @Test
  fun `listeners hear which kind changed, until they close`() {
    val heard = mutableListOf<String>()
    val listening = registry.onChange { heard += it.name }

    val registration = registry.register(importers, FileMatch.Extension("png"), 0, Named("a"))
    registration.close()
    listening.close()
    registry.register(importers, FileMatch.Extension("png"), 0, Named("b"))

    assertEquals(listOf("Importer", "Importer"), heard)
  }

  @Test
  fun `a sniffer reads the head only when its extension matches`() {
    var reads = 0
    registry.register(inspectors, FileMatch.Sniffer("json") { true }, 0, Named("s"))
    val other =
      ProjectFile(Path.of("a.txt")) {
        reads++
        ByteArray(0)
      }

    labels(inspectors, other)

    assertEquals(0, reads)
  }
}

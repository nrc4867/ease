package dev.chieppa.ease.cli

import dev.chieppa.ease.cli.template.TemplateSource
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.io.TempDir

/** Templates that are not the shipped ones: a folder of files the command line reads. */
class TemplateFilesTest {
  @TempDir lateinit var work: Path

  private fun template(kind: String, id: String, manifest: String, files: Map<String, String>) {
    val folder = work.resolve("templates/$kind/$id").createDirectories()
    folder.resolve("template.yaml").writeText(manifest)
    files.forEach { (path, text) ->
      folder.resolve("files/$path").also { it.parent.createDirectories() }.writeText(text)
    }
  }

  private fun harness() =
    Harness(work, templates = TemplateSource.directory(work.resolve("templates")))

  @Test
  fun `a manifest that is bad or from a newer Ease ends the command with a message`() {
    for (text in
      listOf("description: [", "formatVersion: 9\ndescription: x\n", "- a\n", "x: 1\n")) {
      template("projects", "odd", text, mapOf("a.txt" to "a"))
      val harness = harness()

      assertEquals(ExitCodes.FAILED, harness.run("new", "odd", "--name", "Moon"), text)

      assertTrue(
        harness.stderr.contains("projects/odd/template.yaml") ||
          harness.stderr.contains("odd/template.yaml")
      )
      assertFalse(harness.stderr.contains("Exception"))
    }
  }

  @Test
  fun `an insertion that points outside the project changes nothing`() {
    work.resolve("victim.txt").writeText("// mark\n")
    template("projects", "plain", "description: a\nformatVersion: 1\n", mapOf("a.txt.tpl" to "a"))
    template(
      "generators",
      "escape",
      "description: e\nformatVersion: 1\ninsertions:\n" +
        "  - file: ../victim.txt\n    marker: \"// mark\"\n    text: x\n",
      mapOf("b.txt" to "b"),
    )
    val harness = harness()
    assertEquals(ExitCodes.OK, harness.run("new", "plain", "--name", "Moon", "--dir", "game"))

    assertEquals(ExitCodes.FAILED, harness.run("generate", "escape", "X", "--dir", "game"))

    assertEquals("// mark\n", work.resolve("victim.txt").readText())
    assertFalse(work.resolve("game/b.txt").exists())
  }
}

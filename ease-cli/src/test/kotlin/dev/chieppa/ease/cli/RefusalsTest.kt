package dev.chieppa.ease.cli

import dev.chieppa.ease.cli.project.ProjectFile
import dev.chieppa.ease.cli.template.Naming
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.io.TempDir

/** What `ease` refuses, with a message, instead of writing a project that cannot build. */
class RefusalsTest {
  @TempDir lateinit var work: Path

  private fun game(): Path {
    val harness = Harness(work)
    assertEquals(ExitCodes.OK, harness.run("new", "empty", "--name", "Moon Garden"), harness.stderr)
    return work.resolve("moon-garden")
  }

  @Test
  fun `a keyword cannot be a package segment and the default package avoids one`() {
    assertFailsWith<CliFailure> { Naming.requirePackage("example.fun") }
    assertFailsWith<CliFailure> { Naming.requirePackage("object.game") }
    assertEquals("example.fun_", Naming.defaultPackage("example", "Fun"))
    assertEquals("example.moongarden", Naming.defaultPackage("example", "Moon Garden"))

    val harness = Harness(work)
    assertEquals(ExitCodes.OK, harness.run("new", "empty", "--name", "Fun"), harness.stderr)
    assertEquals("example.fun_", ProjectFile.read(work.resolve("fun")).packageName)
    assertEquals(ExitCodes.FAILED, harness.run("new", "empty", "--name", "A", "--package", "x.in"))
  }

  @Test
  fun `a game is not generated inside an Ease checkout unless the folder is named`() {
    val checkout = work.resolve("ease").createDirectories()
    checkout.resolve("settings.gradle.kts").writeText("")
    val cli = checkout.resolve("ease-cli").createDirectories()
    cli.resolve("build.gradle.kts").writeText("")

    assertEquals(ExitCodes.FAILED, Harness(checkout).run("new", "empty", "--name", "Moon"))
    assertFalse(checkout.resolve("moon").exists())

    assertEquals(
      ExitCodes.OK,
      Harness(checkout).run("new", "empty", "--name", "Moon", "--dir", "../moon"),
    )
    assertTrue(work.resolve("moon/settings.gradle.kts").exists())
  }

  @Test
  fun `a project file that is not valid ends the command with a message`() {
    val game = game()
    val harness = Harness(work)
    val bad =
      listOf(
        "not: [valid",
        "formatVersion: 99\nname: x\n",
        "formatVersion: 1\nname: 5\n",
        "- a\n- b\n",
      )
    for (text in bad) {
      game.resolve(ProjectFile.FILE_NAME).writeText(text)
      assertEquals(
        ExitCodes.FAILED,
        harness.run("generate", "system", "Drift", "--dir", "moon-garden"),
      )
    }
    assertTrue(harness.stderr.contains("ease-project.yaml"))
    assertFalse(harness.stderr.contains("Exception"))
  }

  @Test
  fun `project file fields that could leave the project or not compile are refused on read`() {
    val game = game()
    val original = game.resolve(ProjectFile.FILE_NAME).readText()
    val harness = Harness(work)

    for ((field, value) in
      listOf("module" to "../other", "package" to "a.if", "className" to "x y")) {
      val edited =
        original.lines().joinToString("\n") {
          if (it.startsWith("$field:")) "$field: \"$value\"" else it
        }
      game.resolve(ProjectFile.FILE_NAME).writeText(edited)
      assertEquals(
        ExitCodes.FAILED,
        harness.run("generate", "system", "Drift", "--dir", "moon-garden"),
        field,
      )
    }
    assertFalse(work.resolve("other").exists())
  }

  @Test
  fun `names that clash with engine or template names are refused`() {
    val harness = Harness(work)

    assertEquals(ExitCodes.FAILED, harness.run("new", "empty", "--name", "Game Module"))
    assertEquals(ExitCodes.FAILED, harness.run("new", "empty", "--name", "Spawn"))
    game()
    for (name in listOf("Mesh", "Local Transform", "Phase", "Iterating")) {
      val generator = if (name == "Iterating") "system" else "component"
      assertEquals(
        ExitCodes.FAILED,
        harness.run("generate", generator, name, "--dir", "moon-garden"),
        name,
      )
    }
    assertFalse(
      work.resolve("moon-garden/game/src/main/kotlin/example/moongarden/Mesh.kt").exists()
    )
  }
}

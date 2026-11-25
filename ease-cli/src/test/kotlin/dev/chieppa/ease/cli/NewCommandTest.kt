package dev.chieppa.ease.cli

import dev.chieppa.ease.cli.hook.PostGenerateHook
import dev.chieppa.ease.cli.project.ProjectFile
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.walk
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.io.TempDir

class NewCommandTest {
  @TempDir lateinit var work: Path

  private fun newGame(vararg extra: String) {
    val harness = Harness(work)
    assertEquals(
      ExitCodes.OK,
      harness.run("new", "empty", "--name", "Moon Garden", *extra),
      harness.stderr,
    )
  }

  @Test
  fun `a game is written to a folder named for it with every placeholder filled`() {
    newGame()

    val game = work.resolve("moon-garden")
    val source = game.resolve("game/src/main/kotlin/example/moongarden/MoonGarden.kt")
    assertTrue(source.readText().startsWith("package example.moongarden\n"))
    assertTrue(game.resolve("settings.gradle.kts").readText().contains("\"moon-garden\""))
    val leftovers =
      game.walk().filter { it.toFile().isFile && it.fileName.toString() != "gradle-wrapper.jar" }
    assertEquals(emptyList(), leftovers.filter { it.readText().contains("{{") }.toList())
  }

  @Test
  fun `the game pins the exact Ease version and records its project file`() {
    newGame("--ease-version", "0.4.2", "--package", "garden.moon")

    val game = work.resolve("moon-garden")
    val project = ProjectFile.read(game)
    assertEquals("0.4.2", project.easeVersion)
    assertEquals("garden.moon", project.packageName)
    assertEquals("MoonGarden", project.className)
    assertEquals("game", project.module)
    assertEquals("empty", project.template)
    assertTrue(game.resolve("gradle.properties").readText().contains("ease.version=0.4.2"))
  }

  @Test
  fun `the game carries the Gradle wrapper`() {
    newGame()

    assertTrue(work.resolve("moon-garden/gradlew").exists())
    assertTrue(work.resolve("moon-garden/gradle/wrapper/gradle-wrapper.jar").exists())
  }

  @Test
  fun `a folder that already holds files is left alone`() {
    val folder = work.resolve("moon-garden").createDirectories()
    folder.resolve("notes.txt").writeText("mine")

    assertEquals(ExitCodes.FAILED, Harness(work).run("new", "empty", "--name", "Moon Garden"))

    assertEquals("mine", folder.resolve("notes.txt").readText())
    assertFalse(folder.resolve("settings.gradle.kts").exists())
  }

  @Test
  fun `post-generate hooks run on the finished project in order`() {
    val seen = mutableListOf<String>()
    val harness =
      Harness(
        work,
        listOf(
          PostGenerateHook { seen += "first:" + it.file.name },
          PostGenerateHook { seen += "second:" + it.file.name },
        ),
      )

    assertEquals(ExitCodes.OK, harness.run("new", "empty", "--name", "Moon Garden"))

    assertEquals(listOf("first:Moon Garden", "second:Moon Garden"), seen)
  }

  @Test
  fun `a missing name, a bad name and an unknown template are refused`() {
    val harness = Harness(work)

    assertEquals(ExitCodes.FAILED, harness.run("new", "empty"))
    assertEquals(ExitCodes.FAILED, harness.run("new", "empty", "--name", "9 lives"))
    assertEquals(ExitCodes.FAILED, harness.run("new", "nothing", "--name", "Moon"))
    assertEquals(ExitCodes.USAGE, harness.run("frobnicate"))
    assertEquals(emptyList(), work.walk().toList())
  }

  @Test
  fun `templates lists every project template and generator`() {
    val harness = Harness(work)

    assertEquals(ExitCodes.OK, harness.run("templates"))

    val kinds =
      harness.stdout
        .lines()
        .filter { it.isNotBlank() }
        .map { it.split('\t') }
        .map { it[0] to it[1] }
    assertEquals(
      listOf(
        "project" to "empty",
        "generator" to "component",
        "generator" to "module",
        "generator" to "system",
      ),
      kinds,
    )
  }
}

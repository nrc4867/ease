package dev.chieppa.ease.cli

import dev.chieppa.ease.project.TestProject
import dev.chieppa.ease.project.TestProject.Companion.guid
import dev.chieppa.ease.project.TestProject.Companion.node
import dev.chieppa.ease.scene.node.OrderKey
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.io.TempDir

class CheckCommandTest {
  @TempDir lateinit var folder: Path

  private val project by lazy { TestProject(folder) }

  @Test
  fun `a project with an error prints one line per finding and ends with status 1`() {
    project.scene("main.scene", guid(2), listOf(node(1, "Rock", asset = guid(9))))
    val harness = Harness(folder)

    val status = harness.run("check")

    assertEquals(ExitCodes.FAILED, status)
    val lines = harness.stdout.lines().filter { it.isNotEmpty() }
    assertTrue(
      lines.first().startsWith("error   dangling-reference    content/main.scene/nodes/rock-"),
      lines.first(),
    )
    assertEquals("ease check: 1 error, 0 warnings", lines.last())
  }

  @Test
  fun `warnings alone end with status 0`() {
    val key = OrderKey.FIRST
    project.scene(
      "main.scene",
      guid(2),
      listOf(node(1, "A", order = key), node(2, "B", order = key)),
    )
    val harness = Harness(folder)

    val status = harness.run("check")

    assertEquals(ExitCodes.OK, status, harness.stdout)
    assertTrue(harness.stdout.contains("warning equal-order-keys"), harness.stdout)
    assertTrue(harness.stdout.contains("ease check: 0 errors, 1 warning"), harness.stdout)
  }

  @Test
  fun `fix says what it changed and leaves a project the check passes`() {
    val key = OrderKey.FIRST
    project.scene(
      "main.scene",
      guid(2),
      listOf(node(1, "A", order = key), node(2, "B", order = key)),
    )
    val harness = Harness(folder)

    val status = harness.run("check", "--fix")

    assertEquals(ExitCodes.OK, status, harness.stdout)
    assertTrue(harness.stdout.lines().any { it.startsWith("fixed  ") }, harness.stdout)
    assertTrue(harness.stdout.contains("ease check: no problems"), harness.stdout)
  }

  @Test
  fun `a folder that does not exist is a usage failure with a message`() {
    val harness = Harness(folder)

    val status = harness.run("check", "--dir", folder.resolve("nowhere").toString())

    assertEquals(ExitCodes.FAILED, status)
    assertTrue(harness.stderr.contains("is not a folder"), harness.stderr)
    assertTrue(Files.isDirectory(folder))
  }
}

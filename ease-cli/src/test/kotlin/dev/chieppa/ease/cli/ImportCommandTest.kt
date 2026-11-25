package dev.chieppa.ease.cli

import dev.chieppa.ease.project.TestProject
import dev.chieppa.ease.project.TestProject.Companion.guid
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.io.TempDir

class ImportCommandTest {
  @TempDir lateinit var folder: Path

  private val project by lazy { TestProject(folder) }

  @Test
  fun `a project no importer matches imports cleanly`() {
    project.asset("notes.txt", "hello", guid(1))
    val harness = Harness(folder)

    val status = harness.run("import")

    assertEquals(ExitCodes.OK, status, harness.stdout)
    assertTrue(harness.stdout.contains("ease import: 0 imported, 0 up to date, 0 problems"))
  }

  @Test
  fun `a source without a meta file is an error unless adopted`() {
    Files.createDirectories(folder.resolve("content"))
    Files.writeString(folder.resolve("content/new.txt"), "x")
    val harness = Harness(folder)

    assertEquals(ExitCodes.FAILED, harness.run("import"))
    assertEquals(ExitCodes.OK, harness.run("import", "--adopt"))
    assertTrue(Files.exists(folder.resolve("content/new.txt.meta")))
  }
}

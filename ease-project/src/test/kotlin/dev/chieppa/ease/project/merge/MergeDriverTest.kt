package dev.chieppa.ease.project.merge

import dev.chieppa.ease.project.TestGit
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.io.TempDir

class MergeDriverTest {
  @TempDir lateinit var folder: Path

  private val driver by lazy { MergeDriver(TestGit.isolated(folder)) }

  private fun merge(base: String, ours: String, theirs: String): Pair<Boolean, String> {
    val ancestor = folder.resolve("base.yaml").also { Files.writeString(it, base) }
    val current = folder.resolve("ours.yaml").also { Files.writeString(it, ours) }
    val other = folder.resolve("theirs.yaml").also { Files.writeString(it, theirs) }
    return driver.merge(ancestor, current, other) to Files.readString(current)
  }

  @Test
  fun `neighbouring keys that a line merge stops on merge`() {
    val (clean, text) = merge("a: 1\nc: 3\n", "a: 1\nb: 2\nc: 3\n", "a: 1\nbb: 5\nc: 3\n")

    assertTrue(clean)
    assertEquals("a: 1\nb: 2\nbb: 5\nc: 3\n", text)
  }

  @Test
  fun `a value both sides changed leaves ordinary conflict markers and never picks a side`() {
    val (clean, text) = merge("x: 1\n", "x: 2\n", "x: 3\n")

    assertFalse(clean)
    assertTrue(text.contains("<<<<<<< ours") && text.contains("x: 2") && text.contains("x: 3"))
    assertTrue(text.contains(">>>>>>> theirs"))
  }

  @Test
  fun `a file added on both sides with different content conflicts`() {
    val (clean, text) = merge("", "guid: a\n", "guid: b\n")

    assertFalse(clean)
    assertTrue(text.contains("<<<<<<<"))
  }

  @Test
  fun `text that is not YAML falls back to the line merge`() {
    val (clean, text) = merge("a\nb\nc\n", "A\nb\nc\n", "a\nb\nC\n")

    assertTrue(clean)
    assertEquals("A\nb\nC\n", text)
  }
}

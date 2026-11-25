package dev.chieppa.ease.project.check

import dev.chieppa.ease.project.TestGit
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

class ProjectCheckerTest {
  @TempDir lateinit var folder: Path

  private val project by lazy { TestProject(folder) }

  private fun report() = ProjectChecker(folder, TestGit.isolated(folder)).check()

  @Test
  fun `a sound project has no findings`() {
    project.asset("rock.txt", "rock", guid(1))
    project.scene(
      "main.scene",
      guid(2),
      listOf(node(1, "Hall"), node(2, "Rock", asset = guid(1), parent = 1)),
    )

    assertEquals(emptyList(), report().findings)
    assertEquals("no problems", report().summary())
  }

  @Test
  fun `a sidecar without a source and a source without a sidecar are named by file`() {
    project.asset("a.txt", "a", guid(1))
    Files.writeString(folder.resolve("content/loose.txt"), "x")
    Files.delete(folder.resolve("content/a.txt"))

    val report = report()

    val byKind = report.findings.associate { it.kind to it.file }
    assertEquals("content/a.txt.meta", byKind[FindingKind.ORPHAN_SIDECAR])
    assertEquals("content/loose.txt", byKind[FindingKind.MISSING_SIDECAR])
    assertTrue(!report.passed)
  }

  @Test
  fun `the same GUID in two sidecars is one finding on the second`() {
    project.asset("a.txt", "a", guid(1))
    project.asset("b.txt", "b", guid(1))

    val finding = report().findings.single()

    assertEquals(FindingKind.DUPLICATE_GUID, finding.kind)
    assertEquals("content/b.txt.meta", finding.file)
    assertTrue(finding.message.contains("content/a.txt.meta"), finding.message)
  }

  @Test
  fun `a reference to an asset the project lacks is reported in the node file`() {
    project.scene("main.scene", guid(2), listOf(node(1, "Rock", asset = guid(9))))

    val finding = report().findings.single()

    assertEquals(FindingKind.DANGLING_REFERENCE, finding.kind)
    assertTrue(finding.file.startsWith("content/main.scene/nodes/rock-"), finding.file)
    assertTrue(finding.message.contains(guid(9).toString()), finding.message)
  }

  @Test
  fun `a missing parent and a parent loop are errors`() {
    project.scene(
      "main.scene",
      guid(2),
      listOf(
        node(1, "Orphan", parent = 77),
        node(2, "Ping", parent = 3),
        node(3, "Pong", parent = 2),
      ),
    )

    val kinds = report().errors.map { it.kind }.toSet()

    assertEquals(setOf(FindingKind.MISSING_PARENT, FindingKind.PARENT_CYCLE), kinds)
  }

  @Test
  fun `siblings with one order key are a warning that does not fail the check`() {
    val key = OrderKey.FIRST
    project.scene(
      "main.scene",
      guid(2),
      listOf(
        node(1, "Hall"),
        node(2, "A", parent = 1, order = key),
        node(3, "B", parent = 1, order = key),
      ),
    )

    val report = report()

    assertEquals(
      listOf(FindingKind.EQUAL_ORDER_KEYS to Severity.WARNING),
      report.findings.map { it.kind to it.severity },
    )
    assertTrue(report.passed)
  }

  @Test
  fun `a node file that does not read is named`() {
    project.scene("main.scene", guid(2), listOf(node(1, "Hall")))
    Files.writeString(folder.resolve("content/main.scene/nodes/broken.yaml"), "formatVersion: [")

    val finding = report().findings.single()

    assertEquals(FindingKind.UNREADABLE_SCENE_FILE, finding.kind)
    assertEquals("content/main.scene/nodes/broken.yaml", finding.file)
  }

  @Test
  fun `conflict markers are named once and a conflicted sidecar hides the references to it`() {
    project.asset("shared.txt", "s", guid(1))
    Files.writeString(
      folder.resolve("content/shared.txt.meta"),
      "<<<<<<< ours\nformatVersion: 1\nguid: ${guid(1)}\n=======\n" +
        "formatVersion: 1\nguid: ${guid(5)}\n>>>>>>> theirs\n",
    )
    project.scene(
      "main.scene",
      guid(2),
      listOf(
        node(1, "A", asset = guid(1)),
        node(2, "B", asset = guid(5), order = OrderKey.after(OrderKey.FIRST)),
      ),
    )

    val report = report()

    assertEquals(listOf(FindingKind.CONFLICT_MARKERS), report.findings.map { it.kind })
    assertEquals("content/shared.txt.meta", report.findings.single().file)
  }

  @Test
  fun `a binary asset outside Git LFS is an error and a rule for it settles it`() {
    TestGit.isolated(folder).require("init", "--quiet")
    Files.write(folder.resolve("hero.png"), byteArrayOf(0, 1, 2))

    val before = report().errors.single()
    Files.writeString(
      folder.resolve(".gitattributes"),
      "*.png filter=lfs diff=lfs merge=lfs -text\n",
    )

    assertEquals(FindingKind.LFS_UNCOVERED, before.kind)
    assertEquals("hero.png", before.file)
    assertEquals(emptyList(), report().errors)
  }

  @Test
  fun `a folder that is not a git repository has no git findings`() {
    Files.write(folder.resolve("hero.png"), byteArrayOf(0, 1, 2))

    assertEquals(emptyList(), report().findings)
  }
}

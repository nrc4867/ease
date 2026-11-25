package dev.chieppa.ease.cli

import dev.chieppa.ease.cli.hook.GitInitHook
import dev.chieppa.ease.project.TestGit
import dev.chieppa.ease.project.TestProject
import dev.chieppa.ease.project.TestProject.Companion.guid
import dev.chieppa.ease.project.TestProject.Companion.node
import dev.chieppa.ease.scene.node.LocalId
import dev.chieppa.ease.scene.node.OrderKey
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.io.TempDir

/**
 * Two branches of a game made by `ease new`, in real git with the merge driver running this command
 * line: each imports the same file, each deletes an asset the other places, and both edit one node.
 * The merge leaves one conflict, the check names every problem, and the fix leaves a project the
 * check passes.
 */
class ProjectMergeTest {
  @TempDir lateinit var work: Path

  private val root by lazy { work.resolve("moon") }
  private val git by lazy { TestGit.isolated(root) }
  private val project by lazy { TestProject(root) }

  /** A script that runs this command line from the test's own classpath, as git runs `ease`. */
  private fun launcher(): String {
    val java = Path.of(System.getProperty("java.home"), "bin", "java").toString()
    val script = work.resolve("ease-test.sh")
    val classpath = System.getProperty("java.class.path").replace('\\', '/')
    val text =
      "#!/bin/sh\nexec '${java.replace('\\', '/')}' -cp '$classpath' " +
        "dev.chieppa.ease.cli.MainKt \"\$@\"\n"
    Files.writeString(script, text)
    script.toFile().setExecutable(true)
    return script.toString().replace('\\', '/')
  }

  private fun checkOutput(vararg words: String): Pair<Int, String> {
    val harness = Harness(root)
    val status = harness.run(*words)
    return status to harness.stdout + harness.stderr
  }

  private fun String.isDeletedReference(node: String, meta: String) =
    contains("dangling-reference") && contains(node) && contains("$meta was deleted in")

  private fun commit(message: String) = TestGit.commitAll(git, message)

  private fun addNode(id: Long, name: String, asset: Long, order: OrderKey) {
    val nodes = project.readScene("main.scene").nodes.values
    project.saveScene(
      "main.scene",
      nodes + node(id, name, asset = guid(asset), parent = 1, order = order),
    )
  }

  private fun addLampComponent(name: String) {
    val nodes = project.readScene("main.scene").nodes.values
    val lamp = nodes.first { it.id == LocalId(2) }
    val changed = lamp.withField(name, "on", JsonPrimitive(true))
    project.saveScene("main.scene", nodes.filter { it.id != lamp.id } + changed)
  }

  @Test
  fun `two branches that import one file and delete what the other places merge, check and fix`() {
    val hook = GitInitHook(CliOutput(System.out, System.err), launcher()) { TestGit.isolated(it) }
    val created = Harness(work, listOf(hook))
    assertEquals(ExitCodes.OK, created.run("new", "empty", "--name", "Moon"), created.stderr)
    val first = OrderKey.FIRST
    project.asset("a.txt", "a", guid(1))
    project.asset("b.txt", "b", guid(2))
    project.scene(
      "main.scene",
      guid(3),
      listOf(node(1, "Hall"), node(2, "Lamp", parent = 1, order = first)),
    )
    commit("base")

    git.require("checkout", "--quiet", "-b", "left")
    project.asset("shared.txt", "shared", guid(10))
    Files.delete(root.resolve("content/a.txt"))
    Files.delete(root.resolve("content/a.txt.meta"))
    addNode(3, "Shiny", 10, OrderKey.after(first))
    addNode(4, "BeeBox", 2, OrderKey.after(OrderKey.after(first)))
    addLampComponent("test.Alpha")
    commit("left")

    git.require("checkout", "--quiet", "-b", "right", "main")
    project.asset("shared.txt", "shared", guid(20))
    Files.delete(root.resolve("content/b.txt"))
    Files.delete(root.resolve("content/b.txt.meta"))
    addNode(5, "Glossy", 20, OrderKey.after(first))
    addNode(6, "AntHill", 1, OrderKey.after(OrderKey.after(OrderKey.after(first))))
    addLampComponent("test.Beta")
    commit("right")

    val merge = git.run("merge", "--no-edit", "left")

    assertNotEquals(0, merge.exitCode, merge.output)
    assertEquals(
      "content/shared.txt.meta",
      git.require("diff", "--name-only", "--diff-filter=U").trim(),
    )

    val (status, report) = checkOutput("check")
    assertEquals(ExitCodes.FAILED, status, report)
    val lines = report.lines()
    assertEquals(
      1,
      lines.count { it.contains("conflict-markers") && it.contains("shared.txt.meta") },
    )
    assertEquals(1, lines.count { it.isDeletedReference("beebox-", "content/b.txt.meta") })
    assertEquals(1, lines.count { it.isDeletedReference("anthill-", "content/a.txt.meta") })
    assertEquals(1, lines.count { it.startsWith("warning") && it.contains("equal-order-keys") })
    assertEquals("ease check: 3 errors, 1 warning", lines.last { it.isNotEmpty() })

    val (fixStatus, fixed) = checkOutput("check", "--fix")

    assertEquals(ExitCodes.OK, fixStatus, fixed)
    assertTrue(fixed.contains("fixed  content/shared.txt.meta: kept GUID"), fixed)
    assertTrue(fixed.contains("restored"), fixed)
    assertTrue(fixed.contains("order key set to"), fixed)
    assertTrue(fixed.contains("ease check: no problems"), fixed)
    git.require("add", "--all")
    git.require("commit", "--quiet", "--no-verify", "--no-edit")
    assertEquals("", git.require("status", "--porcelain").trim())
    assertEquals(ExitCodes.OK, checkOutput("check").first)
    val lamp =
      root.resolve("content/main.scene/nodes").toFile().listFiles().first {
        it.name.startsWith("lamp-")
      }
    assertTrue(
      lamp.readText().contains("test.Alpha") && lamp.readText().contains("test.Beta"),
      lamp.readText(),
    )
    assertTrue(
      Files.exists(root.resolve("content/a.txt")) && Files.exists(root.resolve("content/b.txt"))
    )
  }
}

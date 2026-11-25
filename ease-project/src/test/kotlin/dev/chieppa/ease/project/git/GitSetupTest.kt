package dev.chieppa.ease.project.git

import dev.chieppa.ease.project.TestGit
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.io.TempDir

class GitSetupTest {
  @TempDir lateinit var folder: Path

  @Test
  fun `a folder becomes a repository with its files, merge driver, hook and first commit`() {
    Files.writeString(folder.resolve("readme.txt"), "hello")
    val git = TestGit.isolated(folder)

    val result = GitSetup(git, "/opt/ease/bin/ease").apply(folder, "Create Moon")

    assertTrue(result.committed, result.notes.toString())
    assertEquals("Create Moon", git.require("log", "-1", "--format=%s").trim())
    assertEquals("main", git.require("symbolic-ref", "--short", "HEAD").trim())
    assertEquals(
      "'/opt/ease/bin/ease' merge-driver %O %A %B %P",
      git.require("config", "--local", "merge.ease.driver").trim(),
    )
    val tracked = git.require("ls-files").lines().toSet()
    assertTrue(
      setOf(".gitignore", ".gitattributes", "readme.txt").all { it in tracked },
      "$tracked",
    )
    val hook = Files.readString(folder.resolve(".git/hooks/pre-commit"))
    assertTrue(hook.contains("ease_bin='/opt/ease/bin/ease'"), hook)
  }

  @Test
  fun `binary assets are lockable LFS files and scenes use the merge driver`() {
    val git = TestGit.isolated(folder)
    GitSetup(git, "ease").apply(folder, "Create")

    val lfs = git.require("check-attr", "filter", "lockable", "--", "art/hero.png", "sound/a.ogg")
    val driver =
      git.require(
        "check-attr",
        "merge",
        "--",
        "content/cave.scene/nodes/lamp-1.yaml",
        "content/a.prefab/scene.yaml",
        "content/notes.yaml",
      )

    assertEquals(2, lfs.lines().count { it.endsWith(": filter: lfs") })
    assertEquals(2, lfs.lines().count { it.endsWith(": lockable: set") })
    assertEquals(
      listOf("ease", "ease", "unspecified"),
      driver.lines().filter { it.isNotEmpty() }.map { it.substringAfterLast(": ") },
    )
  }

  @Test
  fun `caches, cooked output and per-user state stay out of git`() {
    val git = TestGit.isolated(folder)
    GitSetup(git, "ease").apply(folder, "Create")

    val ignored =
      git.require(
        "check-ignore",
        "--",
        ".ease/cache/x",
        ".ease/user/layout.yaml",
        "build/a",
        "content/a.png",
      )

    assertEquals(
      setOf(".ease/cache/x", ".ease/user/layout.yaml", "build/a"),
      ignored.lines().filter { it.isNotEmpty() }.toSet(),
    )
  }
}

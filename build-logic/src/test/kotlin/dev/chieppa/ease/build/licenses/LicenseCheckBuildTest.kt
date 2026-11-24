package dev.chieppa.ease.build.licenses

import java.io.File
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertContains
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.io.TempDir

/**
 * Runs the license tasks in a throwaway build whose one module depends on a library from a local
 * Maven repository, licensed as each test sets.
 */
class LicenseCheckBuildTest {
  @TempDir lateinit var dir: File

  private fun build(license: String) {
    val lib = dir.resolve("repo/dev/example/lib/1.0").apply { mkdirs() }
    lib
      .resolve("lib-1.0.pom")
      .writeText(
        """
      <project>
        <modelVersion>4.0.0</modelVersion>
        <groupId>dev.example</groupId><artifactId>lib</artifactId><version>1.0</version>
        <licenses><license><name>$license</name></license></licenses>
      </project>
      """
          .trimIndent()
      )
    ZipOutputStream(lib.resolve("lib-1.0.jar").outputStream()).close()
    dir
      .resolve("settings.gradle.kts")
      .writeText(
        """
        dependencyResolutionManagement { repositories { maven { url = uri("repo") } } }
        include("probe")
        """
          .trimIndent()
      )
    dir.resolve("build.gradle.kts").writeText("plugins { id(\"ease.licenses\") }")
    dir.resolve("probe").mkdirs()
    dir
      .resolve("probe/build.gradle.kts")
      .writeText(
        """
        plugins { `java-library` }
        dependencies { implementation("dev.example:lib:1.0") }
        """
          .trimIndent()
      )
    dir.resolve("config/licenses").mkdirs()
    dir.resolve("config/licenses/overrides.txt").writeText("")
    dir.resolve("config/licenses/notice-header.txt").writeText("Probe\n")
  }

  private fun runner(vararg tasks: String) =
    GradleRunner.create()
      .withProjectDir(dir)
      .withPluginClasspath()
      .withArguments(*tasks, "--configuration-cache")

  @Test
  fun `an LGPL dependency fails the check`() {
    build("GNU Lesser General Public License, version 2.1")
    val result = runner("checkLicenses").buildAndFail()
    assertContains(result.output, "dev.example:lib:1.0 is licensed LGPL, which may not ship")
  }

  @Test
  fun `a permissive dependency passes and lands in NOTICE`() {
    build("MIT License")
    runner("generateNotice", "checkLicenses", "checkNotice").build()
    assertContains(dir.resolve("NOTICE").readText(), "dev.example:lib:1.0  MIT")
  }

  @Test
  fun `a stale NOTICE fails the check`() {
    build("MIT License")
    dir.resolve("NOTICE").writeText("Probe\n")
    val result = runner("checkNotice").buildAndFail()
    assertContains(result.output, "NOTICE is stale")
  }
}

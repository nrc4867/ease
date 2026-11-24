package dev.chieppa.ease.build.licenses

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.io.TempDir

class PomLicensesTest {
  @TempDir lateinit var dir: File

  @Test
  fun `reads licenses and the parent`() {
    val pom =
      dir.resolve("lib.pom").apply {
        writeText(
          """
          <project xmlns="http://maven.apache.org/POM/4.0.0">
            <parent><groupId>dev.example</groupId><artifactId>parent</artifactId><version>2</version></parent>
            <licenses><license><name>MIT</name><url>https://opensource.org/license/mit</url></license></licenses>
          </project>
          """
            .trimIndent()
        )
      }
    val read = PomLicenses.read(pom)
    assertEquals(
      listOf(DeclaredLicense("MIT", "https://opensource.org/license/mit")),
      read.licenses,
    )
    assertEquals("dev.example:parent:2", read.parent)
  }

  @Test
  fun `a POM without licenses has none`() {
    val pom = dir.resolve("lib.pom").apply { writeText("<project></project>") }
    assertTrue(PomLicenses.read(pom).licenses.isEmpty())
  }

  @Test
  fun `the notice lists each shipped module with its license`() {
    val shipped = listOf(ShippedLicense("dev.example:lib", "1.0", "MIT", null))
    val notice = Notice.render("Header\r\n", shipped)
    assertTrue(notice.startsWith("Header\n\n"))
    assertTrue(notice.endsWith("dev.example:lib:1.0  MIT\n"))
    assertTrue(Notice.isCurrent(notice.replace("\n", "\r\n"), notice))
  }
}

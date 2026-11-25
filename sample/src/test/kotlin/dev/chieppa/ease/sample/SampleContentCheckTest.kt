package dev.chieppa.ease.sample

import dev.chieppa.ease.project.check.ProjectChecker
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.jupiter.api.io.TempDir

/**
 * `ease check` finds nothing wrong with the sample's content, as a game's pre-commit hook runs it.
 */
class SampleContentCheckTest {
  @TempDir lateinit var folder: Path

  @Test
  fun `ease check passes on the sample's content`() {
    SampleContent(folder)

    val report = ProjectChecker(folder).check()

    assertEquals(emptyList(), report.findings.map { "${it.file}: ${it.message}" })
  }
}

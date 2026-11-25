package dev.chieppa.ease.build.game

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.io.TempDir

class GameDependenciesTest {
  @TempDir lateinit var checkout: File

  private fun checkoutVersion(version: String?) {
    val text = if (version == null) "org.gradle.caching=true\n" else "ease.version=$version\n"
    checkout.resolve(GameDependencies.ENGINE_PROPERTIES).writeText(text)
  }

  @Test
  fun `a pin equal to the checkout's version passes`() {
    checkoutVersion("1.2.3")

    GameDependencies.requirePinMatches(checkout, "1.2.3")
  }

  @Test
  fun `a pin that differs fails naming both versions and the way out`() {
    checkoutVersion("1.2.3")

    val failure =
      assertFailsWith<IllegalStateException> {
        GameDependencies.requirePinMatches(checkout, "1.0.0")
      }

    val message = checkNotNull(failure.message)
    assertTrue("1.0.0" in message && "1.2.3" in message, message)
    assertTrue(GameDependencies.DIR_PROPERTY in message, message)
  }

  @Test
  fun `a checkout with no version or a game with no pin fails`() {
    checkoutVersion(null)

    assertNull(GameDependencies.checkoutVersion(checkout))
    assertFailsWith<IllegalStateException> { GameDependencies.requirePinMatches(checkout, "1.0.0") }
    assertFailsWith<IllegalStateException> { GameDependencies.requirePinMatches(checkout, null) }
  }

  @Test
  fun `the settings and properties templates use the names and defaults the plugins read`() {
    val templates =
      File("../ease-cli/src/main/resources/ease-templates/projects/empty/files").canonicalFile
    val settings = templates.resolve("settings.gradle.kts.tpl").readText()
    val properties = templates.resolve("gradle.properties.tpl").readText()

    val read =
      "providers.gradleProperty(\"${GameDependencies.DIR_PROPERTY}\")" +
        ".getOrElse(\"${GameDependencies.DEFAULT_DIR}\")"
    assertTrue(read in settings, "settings.gradle.kts.tpl does not read $read")
    assertTrue(
      "${GameDependencies.DIR_PROPERTY}=${GameDependencies.DEFAULT_DIR}" in properties,
      "gradle.properties.tpl does not set the Ease folder",
    )
    assertTrue("${GameDependencies.VERSION_PROPERTY}={{easeVersion}}" in properties)
  }
}

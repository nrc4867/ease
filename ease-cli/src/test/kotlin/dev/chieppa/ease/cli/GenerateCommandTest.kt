package dev.chieppa.ease.cli

import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.io.TempDir

class GenerateCommandTest {
  @TempDir lateinit var work: Path

  private val harness by lazy { Harness(work) }
  private val game: Path
    get() = work.resolve("moon-garden")

  private val gameClass: String
    get() = game.resolve("game/src/main/kotlin/example/moongarden/MoonGarden.kt").readText()

  private fun generate(vararg words: String): Int {
    if (!game.exists()) {
      val created = harness.run("new", "empty", "--name", "Moon Garden")
      assertEquals(ExitCodes.OK, created, harness.stderr)
    }
    return harness.run("generate", *words, "--dir", "moon-garden")
  }

  @Test
  fun `a component is written with its doc and serial name and registered with the game`() {
    assertEquals(ExitCodes.OK, generate("component", "Health Points"), harness.stderr)

    val component =
      game.resolve("game/src/main/kotlin/example/moongarden/HealthPoints.kt").readText()
    assertTrue(component.contains("@Doc(\"A health points component.\")"))
    assertTrue(component.contains("@SerialName(\"moongarden.HealthPoints\")"))
    assertTrue(
      gameClass.contains(
        "    registry.component(HealthPoints::class, HealthPoints.serializer())\n    // ease:types"
      )
    )
  }

  @Test
  fun `a system is written and added to the simulation phase with its import in place`() {
    assertEquals(ExitCodes.OK, generate("system", "Drift"), harness.stderr)

    assertTrue(game.resolve("game/src/main/kotlin/example/moongarden/DriftSystem.kt").exists())
    val text = gameClass
    assertTrue(
      text.contains("    systems.add(Phase.SIMULATION, DriftSystem())\n    // ease:systems")
    )
    val imports = text.lines().filter { it.startsWith("import ") }
    assertEquals(imports.sorted(), imports)
    assertTrue("import dev.chieppa.ease.ecs.Phase" in imports)
  }

  @Test
  fun `a module gets its own folder and is included in the settings`() {
    assertEquals(ExitCodes.OK, generate("module", "Inventory"), harness.stderr)

    assertTrue(game.resolve("inventory/build.gradle.kts").readText().contains("ease.game-module"))
    assertTrue(game.resolve("settings.gradle.kts").readText().contains("include(\":inventory\")"))
  }

  @Test
  fun `generating the same thing twice refuses and changes nothing`() {
    assertEquals(ExitCodes.OK, generate("system", "Drift"), harness.stderr)
    val before = gameClass

    assertEquals(ExitCodes.FAILED, generate("system", "Drift"))

    assertEquals(before, gameClass)
  }

  @Test
  fun `a generator that cannot edit its target writes none of its files`() {
    assertEquals(ExitCodes.OK, generate("module", "Inventory"), harness.stderr)
    game.resolve("settings.gradle.kts").writeText("rootProject.name = \"x\"\n")

    assertEquals(ExitCodes.FAILED, generate("module", "Quests"))

    assertFalse(game.resolve("quests").exists())
  }

  @Test
  fun `outside a game project a generator fails`() {
    assertEquals(ExitCodes.FAILED, harness.run("generate", "system", "Drift", "--dir", "nowhere"))
  }
}

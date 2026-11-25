package dev.chieppa.ease.sample

import dev.chieppa.ease.assets.Guid
import dev.chieppa.ease.runtime.GameContent
import dev.chieppa.ease.runtime.GameModule
import dev.chieppa.ease.runtime.HostedWorld
import dev.chieppa.ease.runtime.LocalHost
import dev.chieppa.ease.runtime.loop.FixedStep
import dev.chieppa.ease.scene.document.Instance
import dev.chieppa.ease.scene.document.SceneNode
import dev.chieppa.ease.scene.node.LocalId
import dev.chieppa.ease.scene.node.OrderKey
import dev.chieppa.ease.scene.resolve.SceneProblem
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.io.TempDir

/**
 * What a host does with a scene it cannot fully honor, and with a game started without the content
 * its start scene is in.
 */
class SceneProblemsTest {
  @TempDir lateinit var folder: Path

  private val fixedStep = FixedStep.perSecond()

  /** The sample's content with one more node, [node], in its playfield scene. */
  private fun withNode(node: SceneNode) = SampleContent(folder) { it.with(node) }

  @Test
  fun `a node naming a component type the game does not register stops a debug build`() {
    val glimmer = JsonObject(mapOf("shine" to JsonPrimitive(1)))
    val content =
      withNode(SceneNode(LocalId(EXTRA_ID), "Glimmer cube", null, LAST, mapOf(UNKNOWN to glimmer)))

    val failure =
      assertFailsWith<IllegalStateException> {
        HostedWorld(SampleGame(), fixedStep, assets = content.assets)
      }

    assertTrue(UNKNOWN in failure.message.orEmpty(), failure.message)
    assertTrue("registerSceneTypes" in failure.message.orEmpty(), failure.message)
  }

  @Test
  fun `the local host reports a scene's problems when it is placed and at every reload`() {
    val content =
      withNode(SceneNode(LocalId(EXTRA_ID), "Lost crate", null, LAST, instance = Instance(MISSING)))
    val reported = mutableListOf<SceneProblem>()
    val local = LocalHost(SampleGame(), fixedStep, assets = content.assets)
    local.sceneProblems = { reported += it }

    repeat(QUIET_TICKS) { local.step() }
    val atStart = reported.toList()
    val coral = content.cubes().single { it.name == "Coral cube" }
    content.edit(content.playfield().with(coral.withField(TRANSFORM, "x", JsonPrimitive(1f))))
    repeat(QUIET_TICKS) { local.step() }

    assertEquals(1, atStart.size, atStart.toString())
    assertIs<SceneProblem.MissingAsset>(atStart.single())
    assertEquals(2, reported.size, "reported again by the reload, once")
    assertEquals(1, local.host.sceneProblems.count)
  }

  @Test
  fun `a game that starts from a scene refuses to start without its content folder`() {
    val none = assertFailsWith<IllegalStateException> { GameContent.forGame(SampleGame(), null) }
    val missing =
      assertFailsWith<IllegalStateException> {
        GameContent.forGame(SampleGame(), folder.resolve("nowhere"))
      }

    assertTrue("--content" in none.message.orEmpty(), none.message)
    assertTrue("nowhere" in missing.message.orEmpty(), missing.message)
    assertNull(GameContent.forGame(object : GameModule {}, null), "a game with no scene needs none")
  }

  private companion object {
    const val UNKNOWN = "sample.Glimmer"
    const val TRANSFORM = "ease.LocalTransform"
    const val EXTRA_ID = 0x51ce_0000_0000_0001
    const val QUIET_TICKS = 3
    val LAST = OrderKey("zzn")
    val MISSING = Guid(0, 0x6e0)
  }
}

package dev.chieppa.ease.sample

import dev.chieppa.ease.agent.text
import java.nio.file.Path
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.io.TempDir

/**
 * The agent's replay check over the sample's scene: the scratch world is built from the host's log
 * alone, so it neither places the scene again nor takes the content's file changes from the live
 * world's watcher.
 */
class SampleAgentReplicaTest {
  @TempDir lateinit var folder: Path

  private val started = lazy { SampleAgent(folder) }
  private val agent by started

  @AfterTest
  fun close() {
    if (started.isInitialized()) agent.close()
  }

  @Test
  fun `the replay check passes on the scene and leaves pending file changes to the live world`() {
    val content = agent.content
    val coral = content.squares().single { it.name == "Coral square" }
    content.edit(content.playfield().without(coral.id))

    val result = agent.json(agent.call("test", buildJsonObject { put("check", "replay") }))

    assertEquals("true", result.jsonObject.text("passed"), result.toString())
    var pending = 0
    content.watcher.drain { _, _ -> pending++ }
    assertTrue(pending > 0, "the scratch world took the live world's file changes")
  }
}

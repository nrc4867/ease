package dev.chieppa.ease.agent

import dev.chieppa.ease.agent.server.AgentConstants
import dev.chieppa.ease.agent.server.AgentToken
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

class AgentOptionsTest {
  @Test
  fun `the port defaults to the fixed one and --port picks another`() {
    assertEquals(AgentConstants.DEFAULT_PORT, AgentOptions.parse(emptyArray()).port)
    assertEquals(PORT, AgentOptions.parse(arrayOf("--port", PORT.toString())).port)
  }

  @Test
  fun `--content names the game's content folder`() {
    assertEquals(null, AgentOptions.parse(emptyArray()).content)
    assertEquals(Path.of("content"), AgentOptions.parse(arrayOf("--content", "content")).content)
    assertFailsWith<IllegalStateException> { AgentOptions.parse(arrayOf("--content")) }
  }

  @Test
  fun `an unknown option is refused`() {
    assertFailsWith<IllegalStateException> { AgentOptions.parse(arrayOf("--frames", "3")) }
  }

  @Test
  fun `a token from the environment is used, and a blank one replaced by a new one`() {
    assertEquals("fixed-token", AgentToken.resolve(" fixed-token "))
    assertNotEquals("", AgentToken.resolve(" "))
    assertNotEquals(AgentToken.resolve(null), AgentToken.resolve(null))
  }

  private companion object {
    const val PORT = 50_123
  }
}

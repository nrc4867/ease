package dev.chieppa.ease.player

import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PlayerOptionsTest {
  @Test
  fun `no options run until the window closes`() {
    assertEquals(PlayerOptions(), PlayerOptions.parse(emptyArray()))
  }

  @Test
  fun `frames sets a counted run`() {
    assertEquals(PlayerOptions(frames = 120), PlayerOptions.parse(arrayOf("--frames", "120")))
  }

  @Test
  fun `headless sets a run of ticks with no window`() {
    assertEquals(
      PlayerOptions(headlessTicks = 600),
      PlayerOptions.parse(arrayOf("--headless", "600")),
    )
  }

  @Test
  fun `content names the game's content folder, with any run`() {
    assertEquals(
      PlayerOptions(headlessTicks = 600, content = Path.of("content")),
      PlayerOptions.parse(arrayOf("--content", "content", "--headless", "600")),
    )
    assertFailsWith<IllegalStateException> { PlayerOptions.parse(arrayOf("--content")) }
  }

  @Test
  fun `a run is either counted frames or headless ticks`() {
    assertFailsWith<IllegalStateException> {
      PlayerOptions.parse(arrayOf("--frames", "1", "--headless", "1"))
    }
  }

  @Test
  fun `counts must be given and positive`() {
    assertFailsWith<IllegalStateException> { PlayerOptions.parse(arrayOf("--headless")) }
    assertFailsWith<IllegalStateException> { PlayerOptions.parse(arrayOf("--frames", "0")) }
  }

  @Test
  fun `an unknown option is refused`() {
    assertFailsWith<IllegalStateException> { PlayerOptions.parse(arrayOf("--fast")) }
  }
}

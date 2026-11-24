package dev.chieppa.ease.player

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PlayerOptionsTest {
  @Test
  fun `no options run until the window closes`() {
    assertEquals(PlayerOptions(frames = null), PlayerOptions.parse(emptyArray()))
  }

  @Test
  fun `frames sets a counted run`() {
    assertEquals(PlayerOptions(frames = 120), PlayerOptions.parse(arrayOf("--frames", "120")))
  }

  @Test
  fun `an unknown option is refused`() {
    assertFailsWith<IllegalStateException> { PlayerOptions.parse(arrayOf("--fast")) }
  }
}

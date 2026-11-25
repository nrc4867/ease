package {{package}}

import dev.chieppa.ease.runtime.GameModules
import dev.chieppa.ease.runtime.HostedWorld
import dev.chieppa.ease.runtime.loop.FixedStep
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class {{className}}Test {
  private val hosted = HostedWorld(GameModules.load(), FixedStep.perSecond())

  @Test
  fun `the game on the classpath spawns its cube, camera and light`() {
    assertIs<{{className}}>(GameModules.load())
    assertEquals(3, hosted.world.numEntities)
  }

  @Test
  fun `the world ticks`() {
    hosted.step()

    assertEquals(1L, hosted.tick)
  }
}

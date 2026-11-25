package dev.chieppa.ease.sample

import dev.chieppa.ease.runtime.LocalHost
import dev.chieppa.ease.runtime.loop.FixedStep
import dev.chieppa.ease.testing.AllocationBudget
import java.nio.file.Path
import kotlin.test.Test
import org.junit.jupiter.api.io.TempDir

/**
 * Ticks of the sample from its saved scene, its asset and scene syncs included, allocate nothing.
 */
class SampleTickAllocationTest {
  @TempDir lateinit var folder: Path

  @Test
  fun `a tick of the sample world with its extract allocates nothing`() {
    val sample = SampleWorld(folder)

    AllocationBudget.assertWithin(0, { sample.hosted.step() }, WARMUP_TICKS, MEASURED_TICKS)
  }

  @Test
  fun `a tick of host and client over the loopback, as the player runs it, allocates nothing`() {
    val content = SampleContent(folder)
    val local = LocalHost(SampleGame(), FixedStep.perSecond(), assets = content.assets)

    AllocationBudget.assertWithin(0, { local.step() }, WARMUP_TICKS, MEASURED_TICKS)
  }

  private companion object {
    const val WARMUP_TICKS = 5_000
    const val MEASURED_TICKS = 1_000
  }
}

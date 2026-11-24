package dev.chieppa.ease.sample

import dev.chieppa.ease.testing.AllocationBudget
import kotlin.test.Test

class SampleTickAllocationTest {
  @Test
  fun `a tick of the sample world with its extract allocates nothing`() {
    val sample = SampleWorld()

    AllocationBudget.assertWithin(0, { sample.hosted.step() }, WARMUP_TICKS, MEASURED_TICKS)
  }

  private companion object {
    const val WARMUP_TICKS = 5_000
    const val MEASURED_TICKS = 1_000
  }
}

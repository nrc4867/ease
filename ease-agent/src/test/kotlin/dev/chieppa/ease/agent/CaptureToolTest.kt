package dev.chieppa.ease.agent

import dev.chieppa.ease.agent.tools.ToolNames
import dev.chieppa.ease.agent.tools.ToolRefusal
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest

/** Capture on a machine with no GPU adapter, which needs no GPU itself. */
class CaptureToolTest {
  @Test
  fun `with no GPU adapter capture refuses, saying why, every time`() = runTest {
    var opens = 0
    val bench =
      ToolBench(StandardTestDispatcher(testScheduler)) {
        opens++
        error("WebGPU is not available")
      }

    val first = assertFailsWith<ToolRefusal> { bench.call(ToolNames.CAPTURE) }
    assertFailsWith<ToolRefusal> { bench.call(ToolNames.CAPTURE) }

    assertTrue("WebGPU is not available" in first.message.orEmpty(), first.message)
    assertTrue(opens == 1, "opened $opens times")
  }
}

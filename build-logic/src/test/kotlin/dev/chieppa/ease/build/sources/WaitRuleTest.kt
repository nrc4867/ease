package dev.chieppa.ease.build.sources

import kotlin.test.Test
import kotlin.test.assertEquals

class WaitRuleTest {
  private val rule = WaitRule()

  @Test
  fun `each kind of wait fails in tests`() {
    val file =
      testSource(
        "Thread.sleep(10)",
        "TimeUnit.SECONDS.sleep(1)",
        "delay(100)",
        "composeRule.waitUntil { done }",
        "latch.await(5, TimeUnit.SECONDS)",
        "Awaitility.await().until { done }",
      )

    assertEquals(listOf(1, 2, 3, 4, 5, 6), rule.check(file).map { it.line }.distinct())
  }

  @Test
  fun `a wait fails in main code too`() {
    assertEquals(1, rule.check(mainSource("Thread.sleep(10)")).size)
  }

  @Test
  fun `names that only contain a wait word pass`() {
    val file = testSource("val delayed = 3", "fun relay(x: Int) = x", "latch.await()")

    assertEquals(emptyList(), rule.check(file))
  }
}

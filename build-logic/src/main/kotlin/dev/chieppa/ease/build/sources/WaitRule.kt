package dev.chieppa.ease.build.sources

/**
 * No code, main or test, waits on time passing: async code takes its dispatcher and clock from its
 * constructor, and tests advance those, or a world by ticks.
 */
class WaitRule : SourceRule {
  override fun check(file: SourceFile): List<Finding> {
    if (!file.isKotlinOrJava) return emptyList()
    return PATTERNS.flatMap { (pattern, message) -> lineFindings(file, pattern, message) }
  }

  private companion object {
    const val ADVANCE = "advance an injected dispatcher or clock, or a world by ticks"

    val PATTERNS =
      listOf(
        Regex("""\bThread\.sleep\(""") to "waits on the clock; $ADVANCE",
        Regex("""\bTimeUnit\.\w+\.sleep\(""") to "waits on the clock; $ADVANCE",
        Regex("""\bdelay\(""") to "waits on the clock; $ADVANCE",
        Regex("""\bwaitUntil\w*\s*[({]""") to "polls against the clock; wait for an event",
        Regex("""\bawait\w*\([^)]*,\s*\w*TimeUnit""") to "a timed await; wait for the event",
        Regex("""\bAwaitility\b""") to "polls against the clock; wait for an event",
        Regex("""\bLockSupport\.park\w*\(""") to "waits on the clock; $ADVANCE",
      )
  }
}

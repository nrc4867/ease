package dev.chieppa.ease.memory

/** Whether off-heap memory checks its use: on in a debug build, where assertions are enabled. */
public object MemoryChecks {
  /** True in a debug build. */
  public val enabled: Boolean = MemoryChecks::class.java.desiredAssertionStatus()
}

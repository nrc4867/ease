package dev.chieppa.ease.agent.checks

/**
 * The outcome of one headless check: whether it [passed], the [tick] the world it ran on reached,
 * and the [failures] that explain a fail.
 */
public class CheckResult(
  public val check: String,
  public val passed: Boolean,
  public val tick: Long,
  public val failures: List<String> = emptyList(),
)

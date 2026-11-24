package dev.chieppa.ease.testing;

/**
 * Fails a test when an action allocates more than its budget per call. A call is whatever the
 * action does once: one math operation, one system run, one world tick.
 */
public final class AllocationBudget {
  private AllocationBudget() {}

  /** Fails when {@code action} allocates anything, after the default warm-up. */
  public static AllocationReport assertNoAllocation(Runnable action) {
    return assertWithin(0, action);
  }

  /** Fails when {@code action} allocates more than {@code maxBytesPerCall} on average. */
  public static AllocationReport assertWithin(long maxBytesPerCall, Runnable action) {
    return assertWithin(
        maxBytesPerCall,
        action,
        AllocationMeter.DEFAULT_WARMUP_CALLS,
        AllocationMeter.DEFAULT_MEASURED_CALLS);
  }

  /** Fails when {@code action} allocates more than {@code maxBytesPerCall} on average. */
  public static AllocationReport assertWithin(
      long maxBytesPerCall, Runnable action, int warmupCalls, int measuredCalls) {
    AllocationReport report = AllocationMeter.measure(action, warmupCalls, measuredCalls);
    if (report.bytes() > maxBytesPerCall * report.calls()) {
      throw new AllocationBudgetExceeded(maxBytesPerCall, report);
    }
    return report;
  }
}

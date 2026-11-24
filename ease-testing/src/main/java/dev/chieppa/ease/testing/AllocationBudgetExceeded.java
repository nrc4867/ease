package dev.chieppa.ease.testing;

/** An action allocated more per call than its budget allows. */
public final class AllocationBudgetExceeded extends AssertionError {
  private static final long serialVersionUID = 1L;

  private final transient AllocationReport report;

  /** The failure for {@code report} against a budget of {@code maxBytesPerCall}. */
  public AllocationBudgetExceeded(long maxBytesPerCall, AllocationReport report) {
    super(
        "Allocated "
            + report.bytesPerCall()
            + " bytes per call ("
            + report.bytes()
            + " bytes over "
            + report.calls()
            + " calls); the budget is "
            + maxBytesPerCall
            + " bytes per call");
    this.report = report;
  }

  /** What the action allocated. */
  public AllocationReport report() {
    return report;
  }
}

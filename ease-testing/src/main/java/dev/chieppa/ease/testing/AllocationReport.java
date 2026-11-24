package dev.chieppa.ease.testing;

/**
 * What an action allocated on the measuring thread.
 *
 * @param bytes the bytes allocated over all measured calls
 * @param calls the number of measured calls
 */
public record AllocationReport(long bytes, int calls) {
  /** The bytes allocated per call, rounded up, so any allocation at all reads as at least 1. */
  public long bytesPerCall() {
    return Math.ceilDiv(bytes, calls);
  }
}

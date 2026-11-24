package dev.chieppa.ease.testing;

import com.sun.management.ThreadMXBean;
import java.lang.management.ManagementFactory;

/**
 * Counts the bytes the current thread allocates while it runs an action. Only the calling thread is
 * measured, so work the action hands to other threads is not counted.
 */
public final class AllocationMeter {
  /** Calls made before measuring, so class loading, lazy setup and JIT compilation are done. */
  public static final int DEFAULT_WARMUP_CALLS = 20_000;

  /** Calls measured; a report averages over them. */
  public static final int DEFAULT_MEASURED_CALLS = 10_000;

  private static final ThreadMXBean THREADS = threads();

  private AllocationMeter() {}

  /** Runs {@code action} through the default warm-up, then measures the default number of calls. */
  public static AllocationReport measure(Runnable action) {
    return measure(action, DEFAULT_WARMUP_CALLS, DEFAULT_MEASURED_CALLS);
  }

  /** Runs {@code action} {@code warmupCalls} times, then measures {@code measuredCalls} calls. */
  public static AllocationReport measure(Runnable action, int warmupCalls, int measuredCalls) {
    if (warmupCalls < 0 || measuredCalls < 1) {
      throw new IllegalArgumentException(
          "Needs at least 0 warm-up calls and 1 measured call, got "
              + warmupCalls
              + " and "
              + measuredCalls);
    }
    run(action, warmupCalls);
    long before = THREADS.getCurrentThreadAllocatedBytes();
    run(action, measuredCalls);
    long after = THREADS.getCurrentThreadAllocatedBytes();
    return new AllocationReport(after - before, measuredCalls);
  }

  private static void run(Runnable action, int calls) {
    for (int call = 0; call < calls; call++) {
      action.run();
    }
  }

  private static ThreadMXBean threads() {
    if (!(ManagementFactory.getThreadMXBean() instanceof ThreadMXBean threads)
        || !threads.isThreadAllocatedMemorySupported()) {
      throw new IllegalStateException("This JVM cannot count the bytes a thread allocates");
    }
    threads.setThreadAllocatedMemoryEnabled(true);
    return threads;
  }
}

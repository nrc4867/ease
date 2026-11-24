package dev.chieppa.ease.testing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AllocationMeterTest {
  private static final int ARRAY_LENGTH = 16;
  private static final int MEASURED_CALLS = 1_000;

  /** Where allocating actions store their result, so the allocation escapes and is kept. */
  private static Object sink;

  @Test
  void anActionThatAllocatesNothingMeasuresZero() {
    long[] counter = new long[1];

    AllocationReport report = AllocationMeter.measure(() -> counter[0]++);

    assertEquals(0, report.bytes());
  }

  @Test
  void anAllocatingActionMeasuresAtLeastItsPayloadPerCall() {
    AllocationReport report =
        AllocationMeter.measure(() -> sink = new long[ARRAY_LENGTH], 0, MEASURED_CALLS);

    assertEquals(MEASURED_CALLS, report.calls());
    assertTrue(
        report.bytesPerCall() >= (long) ARRAY_LENGTH * Long.BYTES,
        "measured " + report.bytesPerCall() + " bytes per call");
  }

  @Test
  void bytesPerCallRoundsUpSoAnyAllocationShows() {
    assertEquals(1, new AllocationReport(1, MEASURED_CALLS).bytesPerCall());
    assertEquals(0, new AllocationReport(0, MEASURED_CALLS).bytesPerCall());
  }

  @Test
  void measuringNoCallsIsRejected() {
    assertThrows(IllegalArgumentException.class, () -> AllocationMeter.measure(() -> {}, 0, 0));
  }
}

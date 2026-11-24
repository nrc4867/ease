package dev.chieppa.ease.testing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * A system that moves entities stands in for a world tick: one version updates positions in place,
 * the other allocates a new array every tick.
 */
class AllocationBudgetTest {
  private static final int ENTITIES = 64;
  private static final float STEP = 0.5f;

  /** Where the allocating system keeps each tick's positions, so the allocation escapes. */
  private static float[] lastPositions;

  private final float[] positions = new float[ENTITIES];
  private final float[] velocities = new float[ENTITIES];

  @Test
  void aSystemThatUpdatesInPlaceMeetsAZeroBudget() {
    AllocationReport report = AllocationBudget.assertNoAllocation(this::moveInPlace);

    assertEquals(0, report.bytes());
  }

  @Test
  void aSystemThatAllocatesEveryTickFailsAZeroBudget() {
    AllocationBudgetExceeded failure =
        assertThrows(
            AllocationBudgetExceeded.class,
            () -> AllocationBudget.assertNoAllocation(this::moveIntoNewArray));

    assertTrue(failure.report().bytesPerCall() >= (long) ENTITIES * Float.BYTES);
    assertTrue(failure.getMessage().contains("the budget is 0 bytes per call"));
  }

  @Test
  void aSystemWithinItsBudgetPasses() {
    long budget = 2L * ENTITIES * Float.BYTES;

    AllocationReport report = AllocationBudget.assertWithin(budget, this::moveIntoNewArray);

    assertTrue(report.bytesPerCall() <= budget);
  }

  private void moveInPlace() {
    for (int entity = 0; entity < ENTITIES; entity++) {
      positions[entity] += velocities[entity] * STEP;
    }
  }

  private void moveIntoNewArray() {
    float[] next = new float[ENTITIES];
    for (int entity = 0; entity < ENTITIES; entity++) {
      next[entity] = positions[entity] + velocities[entity] * STEP;
    }
    lastPositions = next;
  }
}

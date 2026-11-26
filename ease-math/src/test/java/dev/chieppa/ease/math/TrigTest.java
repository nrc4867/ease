package dev.chieppa.ease.math;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.chieppa.ease.testing.AllocationBudget;
import org.junit.jupiter.api.Test;

/** Trig agrees with StrictMath to within rounding everywhere and allocates nothing. */
class TrigTest {
  private static final double TOLERANCE = 1.0e-12;

  @Test
  void matchesStrictMathAcrossTurns() {
    for (int step = -4000; step <= 4000; step++) {
      double x = step * 0.00731;
      assertEquals(StrictMath.sin(x), Trig.sin(x), TOLERANCE, "sin " + x);
      assertEquals(StrictMath.cos(x), Trig.cos(x), TOLERANCE, "cos " + x);
      double tan = StrictMath.tan(x);
      assertEquals(tan, Trig.tan(x), TOLERANCE * Math.max(1, tan * tan), "tan " + x);
    }
  }

  @Test
  void largeArgumentsFallBackToStrictMath() {
    assertEquals(StrictMath.sin(1.0e7), Trig.sin(1.0e7));
    assertEquals(StrictMath.cos(-3.0e9), Trig.cos(-3.0e9));
  }

  @Test
  void allocatesNothing() {
    double[] sink = new double[1];
    AllocationBudget.assertNoAllocation(
        () -> sink[0] += Trig.sin(sink[0] + 2.5) + Trig.cos(2.9) + Trig.tan(1.2));
  }
}

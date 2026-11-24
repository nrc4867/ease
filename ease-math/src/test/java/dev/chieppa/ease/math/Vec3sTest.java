package dev.chieppa.ease.math;

import static dev.chieppa.ease.math.MathAssert.EPS;
import static dev.chieppa.ease.math.MathAssert.assertArray;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class Vec3sTest {
  private final float[] data = {9, 1, 2, 3, 4, -5, 6};

  @Test
  void addWritesAtOffsets() {
    float[] dst = new float[4];
    Vec3s.add(data, 1, data, 4, dst, 1);
    assertArray(new float[] {0, 5, -3, 9}, dst);
  }

  @Test
  void addAcceptsDestinationAliasingSource() {
    float[] copy = data.clone();
    Vec3s.add(copy, 1, copy, 4, copy, 1);
    assertArray(new float[] {9, 5, -3, 9, 4, -5, 6}, copy);
  }

  @Test
  void subScaleNegate() {
    float[] dst = new float[3];
    Vec3s.sub(data, 1, data, 4, dst, 0);
    assertArray(new float[] {-3, 7, -3}, dst);
    Vec3s.scale(data, 1, 2, dst, 0);
    assertArray(new float[] {2, 4, 6}, dst);
    Vec3s.negate(data, 1, dst, 0);
    assertArray(new float[] {-1, -2, -3}, dst);
  }

  @Test
  void dotCrossAndLengths() {
    float[] dst = new float[3];
    assertEquals(12f, Vec3s.dot(data, 1, data, 4), EPS);
    Vec3s.cross(data, 1, data, 4, dst, 0);
    assertArray(new float[] {27, 6, -13}, dst);
    assertEquals(14f, Vec3s.lengthSquared(data, 1), EPS);
    assertEquals((float) Math.sqrt(14), Vec3s.length(data, 1), EPS);
    assertEquals((float) Math.sqrt(9 + 49 + 9), Vec3s.distance(data, 1, data, 4), EPS);
  }

  @Test
  void normalizeLerpMinMax() {
    float[] dst = new float[3];
    Vec3s.normalize(data, 1, dst, 0);
    assertEquals(1f, Vec3s.length(dst, 0), EPS);
    Vec3s.normalize(new float[3], 0, dst, 0);
    assertArray(new float[3], dst);
    Vec3s.lerp(data, 1, data, 4, 0.5f, dst, 0);
    assertArray(new float[] {2.5f, -1.5f, 4.5f}, dst);
    Vec3s.min(data, 1, data, 4, dst, 0);
    assertArray(new float[] {1, -5, 3}, dst);
    Vec3s.max(data, 1, data, 4, dst, 0);
    assertArray(new float[] {4, 2, 6}, dst);
  }
}

package dev.chieppa.ease.math;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MathAssert {
  static final float EPS = 1e-4f;

  private MathAssert() {}

  static void assertVec3(Vec3 expected, Vec3 actual) {
    assertTrue(expected.approxEquals(actual, EPS), expected + " but was " + actual);
  }

  static void assertVec3(float x, float y, float z, Vec3 actual) {
    assertVec3(new Vec3(x, y, z), actual);
  }

  static void assertMat4(Mat4 expected, Mat4 actual) {
    assertTrue(expected.approxEquals(actual, EPS), expected + " but was " + actual);
  }

  static void assertArray(float[] expected, float[] actual) {
    assertEquals(expected.length, actual.length);
    for (int i = 0; i < expected.length; i++) {
      assertEquals(expected[i], actual[i], EPS, "index " + i);
    }
  }

  static Quat quatY(float degrees) {
    return Quat.fromAxisAngle(Vec3.UNIT_Y, (float) Math.toRadians(degrees));
  }

  static Transform sampleTransform() {
    Quat q = Quat.fromAxisAngle(new Vec3(1, 2, 3), 0.7f);
    return new Transform(new Vec3(1, -2, 3), q, new Vec3(2, 2, 2));
  }
}

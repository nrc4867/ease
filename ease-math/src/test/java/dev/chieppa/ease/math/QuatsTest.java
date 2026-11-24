package dev.chieppa.ease.math;

import static dev.chieppa.ease.math.MathAssert.EPS;
import static dev.chieppa.ease.math.MathAssert.assertArray;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class QuatsTest {
  private static final float HALF_SQRT2 = (float) Math.sqrt(0.5);

  @Test
  void identityAndFromAxisAngleWriteAtOffset() {
    float[] dst = new float[6];
    Quats.identity(dst, 2);
    assertArray(new float[] {0, 0, 0, 0, 0, 1}, dst);
    Quats.fromAxisAngle(0, 1, 0, (float) Math.PI / 2, dst, 1);
    assertArray(new float[] {0, 0, HALF_SQRT2, 0, HALF_SQRT2, 1}, dst);
  }

  @Test
  void mulOfQuarterTurnsIsHalfTurn() {
    float[] q = new float[4];
    Quats.fromAxisAngle(0, 1, 0, (float) Math.PI / 2, q, 0);
    Quats.mul(q, 0, q, 0, q, 0);
    assertArray(new float[] {0, 1, 0, 0}, q);
  }

  @Test
  void conjugateInverseNormalize() {
    float[] q = {0, 2, 0, 0};
    float[] dst = new float[4];
    Quats.conjugate(q, 0, dst, 0);
    assertArray(new float[] {0, -2, 0, 0}, dst);
    Quats.inverse(q, 0, dst, 0);
    assertArray(new float[] {0, -0.5f, 0, 0}, dst);
    Quats.normalize(q, 0, dst, 0);
    assertArray(new float[] {0, 1, 0, 0}, dst);
    assertEquals(4f, Quats.lengthSquared(q, 0), EPS);
  }

  @Test
  void rotateQuarterTurnAboutYMapsXToMinusZ() {
    float[] q = new float[4];
    Quats.fromAxisAngle(0, 1, 0, (float) Math.PI / 2, q, 0);
    float[] v = {1, 0, 0};
    float[] dst = new float[3];
    Quats.rotate(q, 0, v, 0, dst, 0);
    assertArray(new float[] {0, 0, -1}, dst);
  }

  @Test
  void slerpHalfway() {
    float[] a = new float[4];
    float[] b = new float[4];
    Quats.identity(a, 0);
    Quats.fromAxisAngle(0, 1, 0, (float) Math.PI / 2, b, 0);
    float[] dst = new float[4];
    Quats.slerp(a, 0, b, 0, 0.5f, dst, 0);
    float[] expected = new float[4];
    Quats.fromAxisAngle(0, 1, 0, (float) Math.PI / 4, expected, 0);
    assertArray(expected, dst);
  }
}

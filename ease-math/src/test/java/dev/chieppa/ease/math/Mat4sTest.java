package dev.chieppa.ease.math;

import static dev.chieppa.ease.math.MathAssert.EPS;
import static dev.chieppa.ease.math.MathAssert.assertArray;
import static dev.chieppa.ease.math.MathAssert.quatY;
import static dev.chieppa.ease.math.MathAssert.sampleTransform;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class Mat4sTest {
  private static float[] pack(Mat4 m, int offset) {
    float[] data = new float[offset + 16];
    m.writeTo(data, offset);
    return data;
  }

  private static float[] toArray(Mat4 m) {
    return pack(m, 0);
  }

  @Test
  void constructorsMatchTheImmutableTypes() {
    float[] dst = new float[18];
    Mat4s.identity(dst, 1);
    assertArray(toArray(Mat4.IDENTITY), java.util.Arrays.copyOfRange(dst, 1, 17));
    Mat4s.translation(1, 2, 3, dst, 0);
    assertArray(toArray(Mat4.translation(new Vec3(1, 2, 3))), java.util.Arrays.copyOf(dst, 16));
    Mat4s.scaling(1, 2, 3, dst, 0);
    assertArray(toArray(Mat4.scaling(new Vec3(1, 2, 3))), java.util.Arrays.copyOf(dst, 16));
  }

  @Test
  void rotationAndFromTransformMatchTheImmutableTypes() {
    Transform t = sampleTransform();
    float[] q = new float[5];
    t.rotation().writeTo(q, 1);
    float[] dst = new float[16];
    Mat4s.rotation(q, 1, dst, 0);
    assertArray(toArray(Mat4.rotation(t.rotation())), dst);
    float[] parts = new float[10];
    t.translation().writeTo(parts, 0);
    t.rotation().writeTo(parts, 3);
    t.scale().writeTo(parts, 7);
    Mat4s.fromTransform(parts, 0, parts, 3, parts, 7, dst, 0);
    assertArray(toArray(t.toMat4()), dst);
  }

  @Test
  void mulAtOffsetsAndAliasing() {
    Mat4 a = sampleTransform().toMat4();
    Mat4 b = Mat4.rotation(quatY(33)).mul(Mat4.translation(new Vec3(1, 1, 1)));
    float[] data = new float[32];
    a.writeTo(data, 0);
    b.writeTo(data, 16);
    float[] expected = toArray(a.mul(b));
    float[] dst = new float[17];
    Mat4s.mul(data, 0, data, 16, dst, 1);
    assertArray(expected, java.util.Arrays.copyOfRange(dst, 1, 17));
    Mat4s.mul(data, 0, data, 16, data, 0);
    assertArray(expected, java.util.Arrays.copyOf(data, 16));
  }

  @Test
  void transposeInPlaceAndCopy() {
    float[] m = toArray(sampleTransform().toMat4());
    float[] copy = new float[16];
    Mat4s.transpose(m, 0, copy, 0);
    assertEquals(m[1], copy[4], EPS);
    Mat4s.transpose(copy, 0, copy, 0);
    assertArray(m, copy);
  }

  @Test
  void transformPointDirectionAndVec4() {
    float[] m = toArray(Mat4.fromTransform(new Vec3(10, 0, 0), quatY(90), Vec3.ONE));
    float[] v = {1, 0, 0, 1};
    float[] dst = new float[4];
    Mat4s.transformPoint(m, 0, v, 0, dst, 0);
    assertArray(new float[] {10, 0, -1, 0}, dst);
    Mat4s.transformDirection(m, 0, v, 0, dst, 0);
    assertArray(new float[] {0, 0, -1, 0}, dst);
    Mat4s.transformVec4(m, 0, v, 0, dst, 0);
    assertArray(new float[] {10, 0, -1, 1}, dst);
  }

  @Test
  void inverseAndDeterminant() {
    float[] m = toArray(sampleTransform().toMat4());
    float[] inv = new float[16];
    assertTrue(Mat4Inverse.inverse(m, 0, inv, 0));
    float[] product = new float[16];
    Mat4s.mul(m, 0, inv, 0, product, 0);
    assertArray(toArray(Mat4.IDENTITY), product);
    assertEquals(8f, Mat4Inverse.determinant(m, 0), 1e-3f);
    float[] affine = new float[16];
    assertTrue(Mat4Inverse.inverseAffine(m, 0, affine, 0));
    assertArray(inv, affine);
  }

  @Test
  void singularInverseReturnsFalseAndLeavesDestination() {
    float[] m = toArray(Mat4.scaling(new Vec3(1, 0, 1)));
    float[] dst = new float[16];
    dst[0] = 42;
    assertFalse(Mat4Inverse.inverse(m, 0, dst, 0));
    assertFalse(Mat4Inverse.inverseAffine(m, 0, dst, 0));
    assertEquals(42f, dst[0]);
  }
}

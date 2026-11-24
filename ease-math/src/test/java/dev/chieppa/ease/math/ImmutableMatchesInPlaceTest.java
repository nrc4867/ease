package dev.chieppa.ease.math;

import static dev.chieppa.ease.math.MathAssert.assertArray;
import static dev.chieppa.ease.math.MathAssert.quatY;
import static dev.chieppa.ease.math.MathAssert.sampleTransform;

import org.junit.jupiter.api.Test;

class ImmutableMatchesInPlaceTest {
  private final Mat4 a = sampleTransform().toMat4();
  private final Mat4 b = Mat4.rotation(quatY(33)).mul(Mat4.translation(new Vec3(1, 2, 3)));
  private final Quat qa = Quat.fromAxisAngle(new Vec3(1, 2, 3), 0.9f);
  private final Quat qb = quatY(70);

  private static float[] array(Mat4 m) {
    float[] out = new float[16];
    m.writeTo(out, 0);
    return out;
  }

  private static float[] array(Quat q) {
    float[] out = new float[4];
    q.writeTo(out, 0);
    return out;
  }

  @Test
  void mat4MulMatches() {
    float[] expected = new float[16];
    Mat4s.mul(array(a), 0, array(b), 0, expected, 0);
    assertArray(expected, array(a.mul(b)));
  }

  @Test
  void mat4InversesMatch() {
    float[] general = new float[16];
    Mat4Inverse.inverse(array(a), 0, general, 0);
    assertArray(general, array(a.inverse()));
    float[] affine = new float[16];
    Mat4Inverse.inverseAffine(array(a), 0, affine, 0);
    assertArray(affine, array(a.inverseAffine()));
  }

  @Test
  void mat4TransposeMatches() {
    float[] expected = new float[16];
    Mat4s.transpose(array(a), 0, expected, 0);
    assertArray(expected, array(a.transpose()));
  }

  @Test
  void mat4PointTransformsMatch() {
    float[] p = {1, -2, 3};
    float[] expected = new float[3];
    Mat4s.transformPoint(array(a), 0, p, 0, expected, 0);
    assertArray(expected, floats(a.transformPoint(new Vec3(1, -2, 3))));
    Mat4s.transformDirection(array(a), 0, p, 0, expected, 0);
    assertArray(expected, floats(a.transformDirection(new Vec3(1, -2, 3))));
  }

  @Test
  void mat4ConstructorsMatch() {
    Transform t = sampleTransform();
    float[] parts = new float[10];
    t.translation().writeTo(parts, 0);
    t.rotation().writeTo(parts, 3);
    t.scale().writeTo(parts, 7);
    float[] expected = new float[16];
    Mat4s.fromTransform(parts, 0, parts, 3, parts, 7, expected, 0);
    assertArray(expected, array(t.toMat4()));
    Mat4Cameras.perspective(1f, 1.5f, 0.1f, 50f, expected, 0);
    assertArray(expected, array(Mat4.perspective(1f, 1.5f, 0.1f, 50f)));
    Mat4Cameras.orthographic(-2, 2, -1, 1, 0.5f, 9f, expected, 0);
    assertArray(expected, array(Mat4.orthographic(-2, 2, -1, 1, 0.5f, 9f)));
    float[] views = {1, 2, 3, -4, 0.5f, 6, 0, 1, 0};
    Mat4Cameras.lookAt(views, 0, views, 3, views, 6, expected, 0);
    Mat4 look = Mat4.lookAt(new Vec3(1, 2, 3), new Vec3(-4, 0.5f, 6), Vec3.UNIT_Y);
    assertArray(expected, array(look));
  }

  @Test
  void quatOperationsMatch() {
    float[] expected = new float[4];
    Quats.mul(array(qa), 0, array(qb), 0, expected, 0);
    assertArray(expected, array(qa.mul(qb)));
    Quats.slerp(array(qa), 0, array(qb), 0, 0.3f, expected, 0);
    assertArray(expected, array(qa.slerp(qb, 0.3f)));
    Quats.inverse(array(qa), 0, expected, 0);
    assertArray(expected, array(qa.inverse()));
    float[] rotated = new float[3];
    Quats.rotate(array(qa), 0, new float[] {1, 2, 3}, 0, rotated, 0);
    assertArray(rotated, floats(qa.rotate(new Vec3(1, 2, 3))));
  }

  @Test
  void aabbTransformMatches() {
    Aabb box = new Aabb(new Vec3(-1, -2, -3), new Vec3(4, 5, 6));
    float[] boxData = new float[6];
    box.writeTo(boxData, 0);
    float[] expected = new float[6];
    Aabbs.transform(boxData, 0, array(a), 0, expected, 0);
    float[] actual = new float[6];
    box.transformed(a).writeTo(actual, 0);
    assertArray(expected, actual);
  }

  private static float[] floats(Vec3 v) {
    float[] out = new float[3];
    v.writeTo(out, 0);
    return out;
  }
}

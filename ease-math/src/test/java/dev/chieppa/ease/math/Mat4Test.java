package dev.chieppa.ease.math;

import static dev.chieppa.ease.math.MathAssert.EPS;
import static dev.chieppa.ease.math.MathAssert.assertArray;
import static dev.chieppa.ease.math.MathAssert.assertMat4;
import static dev.chieppa.ease.math.MathAssert.assertVec3;
import static dev.chieppa.ease.math.MathAssert.quatY;
import static dev.chieppa.ease.math.MathAssert.sampleTransform;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class Mat4Test {
  private final Mat4 general = sampleTransform().toMat4().mul(Mat4.perspective(1f, 1.5f, 0.1f, 50));

  @Test
  void identityLeavesPointsAlone() {
    assertVec3(1, 2, 3, Mat4.IDENTITY.transformPoint(new Vec3(1, 2, 3)));
    assertEquals(1f, Mat4.IDENTITY.determinant(), EPS);
  }

  @Test
  void translationMovesPointsButNotDirections() {
    Mat4 m = Mat4.translation(new Vec3(1, 2, 3));
    assertVec3(2, 3, 4, m.transformPoint(Vec3.ONE));
    assertVec3(1, 1, 1, m.transformDirection(Vec3.ONE));
  }

  @Test
  void scalingScalesComponents() {
    Mat4 m = Mat4.scaling(new Vec3(2, 3, 4));
    assertVec3(2, 3, 4, m.transformPoint(Vec3.ONE));
    assertEquals(24f, m.determinant(), EPS);
  }

  @Test
  void rotationAboutYMapsXToMinusZ() {
    assertVec3(0, 0, -1, Mat4.rotation(quatY(90)).transformPoint(Vec3.UNIT_X));
  }

  @Test
  void fromTransformScalesThenRotatesThenTranslates() {
    Mat4 m = Mat4.fromTransform(new Vec3(10, 0, 0), quatY(90), new Vec3(2, 1, 1));
    assertVec3(10, 0, -2, m.transformPoint(Vec3.UNIT_X));
  }

  @Test
  void mulAppliesRightOperandFirst() {
    Mat4 move = Mat4.translation(new Vec3(1, 0, 0));
    Mat4 turn = Mat4.rotation(quatY(90));
    assertVec3(0, 0, -1, turn.mul(move).transformPoint(Vec3.ZERO).add(Vec3.ZERO));
    assertVec3(1, 0, 0, move.mul(turn).transformPoint(Vec3.ZERO));
  }

  @Test
  void transposeSwapsRowsAndColumns() {
    Mat4 m = Mat4.readFrom(sequence(), 0);
    Mat4 t = m.transpose();
    assertEquals(m.m01(), t.m10());
    assertEquals(m.m32(), t.m23());
    assertMat4(m, t.transpose());
  }

  @Test
  void determinantOfKnownMatrices() {
    assertEquals(-2f, Mat4.scaling(new Vec3(1, 2, -1)).determinant(), EPS);
    assertEquals(0f, Mat4.readFrom(sequence(), 0).determinant(), EPS);
  }

  @Test
  void inverseTimesMatrixIsIdentity() {
    assertMat4(Mat4.IDENTITY, general.mul(general.inverse()));
    assertMat4(Mat4.IDENTITY, general.inverse().mul(general));
  }

  @Test
  void affineInverseMatchesGeneralInverse() {
    Mat4 affine = sampleTransform().toMat4();
    assertMat4(affine.inverse(), affine.inverseAffine());
    assertMat4(Mat4.IDENTITY, affine.mul(affine.inverseAffine()));
  }

  @Test
  void singularMatricesThrow() {
    Mat4 flat = Mat4.scaling(new Vec3(1, 0, 1));
    assertThrows(ArithmeticException.class, flat::inverse);
    assertThrows(ArithmeticException.class, flat::inverseAffine);
  }

  @Test
  void transformVec4UsesAllRows() {
    Vec4 v = Mat4.translation(new Vec3(1, 2, 3)).transform(new Vec4(1, 1, 1, 2));
    assertEquals(new Vec4(3, 5, 7, 2), v);
  }

  @Test
  void writeToIsColumnMajor() {
    float[] data = new float[17];
    Mat4.translation(new Vec3(7, 8, 9)).writeTo(data, 1);
    assertArray(new float[] {7, 8, 9, 1}, new float[] {data[13], data[14], data[15], data[16]});
    assertMat4(Mat4.translation(new Vec3(7, 8, 9)), Mat4.readFrom(data, 1));
  }

  private static float[] sequence() {
    float[] data = new float[16];
    for (int i = 0; i < 16; i++) {
      data[i] = i + 1;
    }
    return data;
  }
}

package dev.chieppa.ease.math;

import static dev.chieppa.ease.math.MathAssert.EPS;
import static dev.chieppa.ease.math.MathAssert.assertMat4;
import static dev.chieppa.ease.math.MathAssert.assertVec3;
import static dev.chieppa.ease.math.MathAssert.quatY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class QuatTest {
  @Test
  void identityDoesNotRotate() {
    assertVec3(1, 2, 3, Quat.IDENTITY.rotate(new Vec3(1, 2, 3)));
  }

  @Test
  void quarterTurnAboutYMapsXToMinusZ() {
    assertVec3(0, 0, -1, quatY(90).rotate(Vec3.UNIT_X));
  }

  @Test
  void fromAxisAngleNormalizesTheAxis() {
    Quat q = Quat.fromAxisAngle(new Vec3(0, 5, 0), (float) Math.PI);
    assertEquals(1f, q.y(), EPS);
    assertEquals(0f, q.w(), EPS);
    assertEquals(Quat.IDENTITY, Quat.fromAxisAngle(Vec3.ZERO, 1));
  }

  @Test
  void mulAppliesRightOperandFirst() {
    Quat aboutX = Quat.fromAxisAngle(Vec3.UNIT_X, (float) Math.PI / 2);
    Quat combined = quatY(90).mul(aboutX);
    assertVec3(quatY(90).rotate(aboutX.rotate(Vec3.UNIT_Y)), combined.rotate(Vec3.UNIT_Y));
  }

  @Test
  void conjugateAndInverseUndoRotation() {
    Quat q = Quat.fromAxisAngle(new Vec3(1, 2, 3), 1.1f);
    Vec3 v = new Vec3(4, -1, 2);
    assertVec3(v, q.conjugate().rotate(q.rotate(v)));
    assertTrue(q.mul(q.inverse()).approxEquals(Quat.IDENTITY, EPS));
    Quat scaled = new Quat(0, 2, 0, 0);
    assertEquals(-0.5f, scaled.inverse().y(), EPS);
  }

  @Test
  void normalizeGivesUnitLengthAndIdentityForZero() {
    Quat n = new Quat(1, 2, 3, 4).normalize();
    assertEquals(1f, n.x() * n.x() + n.y() * n.y() + n.z() * n.z() + n.w() * n.w(), EPS);
    assertEquals(Quat.IDENTITY, new Quat(0, 0, 0, 0).normalize());
  }

  @Test
  void slerpHalfwayIsHalfTheAngle() {
    Quat half = Quat.IDENTITY.slerp(quatY(90), 0.5f);
    assertTrue(half.approxEquals(quatY(45), EPS));
  }

  @Test
  void slerpEndpoints() {
    Quat target = quatY(120);
    assertTrue(Quat.IDENTITY.slerp(target, 0).approxEquals(Quat.IDENTITY, EPS));
    assertTrue(Quat.IDENTITY.slerp(target, 1).approxEquals(target, EPS));
  }

  @Test
  void slerpTakesTheShortestPath() {
    Quat negated = new Quat(0, 0, 0, -1);
    Quat result = quatY(10).slerp(negated, 0.5f);
    assertVec3(quatY(5).rotate(Vec3.UNIT_X), result.rotate(Vec3.UNIT_X));
  }

  @Test
  void slerpOfNearlyParallelRotationsStaysUnit() {
    Quat result = quatY(0).slerp(quatY(0.001f), 0.5f);
    assertTrue(result.approxEquals(quatY(0.0005f), EPS));
  }

  @Test
  void toMat4RotatesLikeTheQuaternion() {
    Quat q = Quat.fromAxisAngle(new Vec3(1, 1, 0), 0.8f);
    Vec3 v = new Vec3(1, 2, 3);
    assertVec3(q.rotate(v), q.toMat4().transformDirection(v));
    assertMat4(Mat4.IDENTITY, Quat.IDENTITY.toMat4());
  }

  @Test
  void arrayRoundTripAtOffset() {
    float[] data = new float[6];
    quatY(30).writeTo(data, 2);
    assertEquals(quatY(30), Quat.readFrom(data, 2));
  }
}

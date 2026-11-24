package dev.chieppa.ease.math;

import static dev.chieppa.ease.math.MathAssert.EPS;
import static dev.chieppa.ease.math.MathAssert.assertMat4;
import static dev.chieppa.ease.math.MathAssert.assertVec3;
import static dev.chieppa.ease.math.MathAssert.quatY;
import static dev.chieppa.ease.math.MathAssert.sampleTransform;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TransformTest {
  @Test
  void identityLeavesPointsAlone() {
    assertVec3(1, 2, 3, Transform.IDENTITY.transformPoint(new Vec3(1, 2, 3)));
    assertMat4(Mat4.IDENTITY, Transform.IDENTITY.toMat4());
  }

  @Test
  void transformPointScalesRotatesThenTranslates() {
    Transform t = new Transform(new Vec3(10, 0, 0), quatY(90), new Vec3(2, 2, 2));
    assertVec3(10, 0, -2, t.transformPoint(Vec3.UNIT_X));
  }

  @Test
  void toMat4AgreesWithTransformPoint() {
    Transform t = sampleTransform();
    Vec3 p = new Vec3(0.5f, -1, 2);
    assertVec3(t.transformPoint(p), t.toMat4().transformPoint(p));
  }

  @Test
  void composeMatchesMatrixProduct() {
    Transform parent = sampleTransform();
    Transform child = new Transform(new Vec3(0, 1, 0), quatY(40), Vec3.ONE);
    Vec3 p = new Vec3(1, 2, 3);
    assertVec3(
        parent.toMat4().mul(child.toMat4()).transformPoint(p),
        parent.compose(child).transformPoint(p));
  }

  @Test
  void composeWithIdentityChangesNothing() {
    Transform t = sampleTransform();
    assertVec3(t.translation(), t.compose(Transform.IDENTITY).translation());
    assertTrue(t.rotation().approxEquals(Transform.IDENTITY.compose(t).rotation(), EPS));
  }

  @Test
  void inverseUndoesTransformPoint() {
    Transform t = sampleTransform();
    Vec3 p = new Vec3(4, 5, 6);
    assertVec3(p, t.inverse().transformPoint(t.transformPoint(p)));
    assertMat4(Mat4.IDENTITY, t.toMat4().mul(t.inverse().toMat4()));
  }

  @Test
  void interpolateBlendsEachPart() {
    Transform a = Transform.IDENTITY;
    Transform b = new Transform(new Vec3(2, 4, 6), quatY(90), new Vec3(3, 3, 3));
    Transform mid = Transform.interpolate(a, b, 0.5f);
    assertVec3(1, 2, 3, mid.translation());
    assertVec3(2, 2, 2, mid.scale());
    assertTrue(mid.rotation().approxEquals(quatY(45), EPS));
    assertEquals(a, Transform.interpolate(a, b, 0));
  }
}

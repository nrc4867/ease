package dev.chieppa.ease.math;

import static dev.chieppa.ease.math.MathAssert.EPS;
import static dev.chieppa.ease.math.MathAssert.assertVec3;
import static dev.chieppa.ease.math.MathAssert.quatY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AabbTest {
  private final Aabb box = new Aabb(new Vec3(-1, -2, -3), new Vec3(1, 2, 3));

  @Test
  void ofPointsFindsMinAndMax() {
    Aabb b = Aabb.of(new Vec3(1, 5, -2), new Vec3(-3, 0, 4), new Vec3(0, 2, 2));
    assertEquals(new Vec3(-3, 0, -2), b.min());
    assertEquals(new Vec3(1, 5, 4), b.max());
  }

  @Test
  void ofNoPointsThrows() {
    assertThrows(IllegalArgumentException.class, Aabb::of);
  }

  @Test
  void unionCoversBoth() {
    Aabb other = new Aabb(new Vec3(0, 0, 0), new Vec3(5, 1, 1));
    Aabb u = box.union(other);
    assertEquals(new Vec3(-1, -2, -3), u.min());
    assertEquals(new Vec3(5, 2, 3), u.max());
  }

  @Test
  void containsIsInclusive() {
    assertTrue(box.contains(Vec3.ZERO));
    assertTrue(box.contains(new Vec3(1, 2, 3)));
    assertFalse(box.contains(new Vec3(1.01f, 0, 0)));
  }

  @Test
  void intersectsOverlappingAndTouchingButNotApart() {
    assertTrue(box.intersects(new Aabb(new Vec3(0, 0, 0), new Vec3(4, 4, 4))));
    assertTrue(box.intersects(new Aabb(new Vec3(1, 2, 3), new Vec3(4, 4, 4))));
    assertFalse(box.intersects(new Aabb(new Vec3(2, 0, 0), new Vec3(4, 4, 4))));
  }

  @Test
  void centerAndExtents() {
    Aabb b = new Aabb(new Vec3(0, 2, 4), new Vec3(2, 6, 10));
    assertVec3(1, 4, 7, b.center());
    assertVec3(1, 2, 3, b.extents());
  }

  @Test
  void transformedByTranslationShiftsTheBox() {
    Aabb moved = box.transformed(Mat4.translation(new Vec3(10, 0, 0)));
    assertVec3(9, -2, -3, moved.min());
    assertVec3(11, 2, 3, moved.max());
  }

  @Test
  void transformedByRotationContainsAllRotatedCorners() {
    Mat4 m = Mat4.fromTransform(new Vec3(1, 2, 3), quatY(35), new Vec3(1, 2, 1));
    Aabb result = box.transformed(m);
    for (int i = 0; i < 8; i++) {
      Vec3 corner =
          new Vec3(
              (i & 1) == 0 ? box.min().x() : box.max().x(),
              (i & 2) == 0 ? box.min().y() : box.max().y(),
              (i & 4) == 0 ? box.min().z() : box.max().z());
      Vec3 p = m.transformPoint(corner);
      assertTrue(result.contains(p.add(new Vec3(EPS, EPS, EPS)).min(result.max())), "corner " + i);
    }
  }

  @Test
  void transformedQuarterTurnSwapsExtents() {
    Aabb result = box.transformed(Mat4.rotation(quatY(90)));
    assertVec3(-3, -2, -1, result.min());
    assertVec3(3, 2, 1, result.max());
  }

  @Test
  void arrayRoundTrip() {
    float[] data = new float[8];
    box.writeTo(data, 2);
    assertEquals(box, Aabb.readFrom(data, 2));
  }
}

package dev.chieppa.ease.math;

import static dev.chieppa.ease.math.MathAssert.EPS;
import static dev.chieppa.ease.math.MathAssert.assertVec3;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class VectorTest {
  private final Vec3 a = new Vec3(1, 2, 3);
  private final Vec3 b = new Vec3(4, -5, 6);

  @Test
  void vec3Arithmetic() {
    assertEquals(new Vec3(5, -3, 9), a.add(b));
    assertEquals(new Vec3(-3, 7, -3), a.sub(b));
    assertEquals(new Vec3(2, 4, 6), a.scale(2));
    assertEquals(new Vec3(-1, -2, -3), a.negate());
    assertEquals(12f, a.dot(b), EPS);
  }

  @Test
  void vec3CrossOfUnitAxes() {
    assertEquals(Vec3.UNIT_Z, Vec3.UNIT_X.cross(Vec3.UNIT_Y));
    assertEquals(new Vec3(27, 6, -13), a.cross(b));
  }

  @Test
  void vec3LengthAndDistance() {
    Vec3 v = new Vec3(3, 4, 12);
    assertEquals(169f, v.lengthSquared(), EPS);
    assertEquals(13f, v.length(), EPS);
    assertEquals(13f, v.distance(Vec3.ZERO), EPS);
  }

  @Test
  void vec3NormalizeGivesUnitLengthAndKeepsZero() {
    assertEquals(1f, new Vec3(3, 4, 12).normalize().length(), EPS);
    assertEquals(Vec3.ZERO, Vec3.ZERO.normalize());
  }

  @Test
  void vec3LerpMinMax() {
    assertVec3(2.5f, -1.5f, 4.5f, a.lerp(b, 0.5f));
    assertEquals(new Vec3(1, -5, 3), a.min(b));
    assertEquals(new Vec3(4, 2, 6), a.max(b));
  }

  @Test
  void vec3ApproxEqualsUsesTolerance() {
    assertTrue(a.approxEquals(new Vec3(1.001f, 2, 3), 0.01f));
    assertFalse(a.approxEquals(new Vec3(1.1f, 2, 3), 0.01f));
  }

  @Test
  void vec3ArrayRoundTripAtOffset() {
    float[] data = new float[5];
    a.writeTo(data, 2);
    assertEquals(a, Vec3.readFrom(data, 2));
    assertEquals(0f, data[1]);
  }

  @Test
  void vec2Operations() {
    Vec2 p = new Vec2(3, 4);
    Vec2 q = new Vec2(1, -2);
    assertEquals(new Vec2(4, 2), p.add(q));
    assertEquals(new Vec2(2, 6), p.sub(q));
    assertEquals(new Vec2(6, 8), p.scale(2));
    assertEquals(new Vec2(-3, -4), p.negate());
    assertEquals(-5f, p.dot(q), EPS);
    assertEquals(25f, p.lengthSquared(), EPS);
    assertEquals(5f, p.length(), EPS);
    assertEquals(5f, p.distance(Vec2.ZERO), EPS);
    assertEquals(new Vec2(0.6f, 0.8f).x(), p.normalize().x(), EPS);
    assertEquals(Vec2.ZERO, Vec2.ZERO.normalize());
    assertEquals(new Vec2(2, 1), p.lerp(q, 0.5f));
    assertEquals(new Vec2(1, -2), p.min(q));
    assertEquals(new Vec2(3, 4), p.max(q));
    assertTrue(p.approxEquals(new Vec2(3.001f, 4), 0.01f));
    float[] data = new float[3];
    p.writeTo(data, 1);
    assertEquals(p, Vec2.readFrom(data, 1));
  }

  @Test
  void vec4Operations() {
    Vec4 p = new Vec4(1, 2, 2, 4);
    Vec4 q = new Vec4(3, 0, -2, 1);
    assertEquals(new Vec4(4, 2, 0, 5), p.add(q));
    assertEquals(new Vec4(-2, 2, 4, 3), p.sub(q));
    assertEquals(new Vec4(2, 4, 4, 8), p.scale(2));
    assertEquals(new Vec4(-1, -2, -2, -4), p.negate());
    assertEquals(3f, p.dot(q), EPS);
    assertEquals(25f, p.lengthSquared(), EPS);
    assertEquals(5f, p.length(), EPS);
    assertEquals(5f, p.distance(Vec4.ZERO), EPS);
    assertEquals(1f, p.normalize().length(), EPS);
    assertEquals(Vec4.ZERO, Vec4.ZERO.normalize());
    assertEquals(new Vec4(2, 1, 0, 2.5f), p.lerp(q, 0.5f));
    assertEquals(new Vec4(1, 0, -2, 1), p.min(q));
    assertEquals(new Vec4(3, 2, 2, 4), p.max(q));
    assertTrue(p.approxEquals(new Vec4(1.001f, 2, 2, 4), 0.01f));
    assertFalse(p.approxEquals(q, 0.01f));
    float[] data = new float[5];
    p.writeTo(data, 1);
    assertEquals(p, Vec4.readFrom(data, 1));
  }
}

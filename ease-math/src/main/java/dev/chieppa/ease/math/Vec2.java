package dev.chieppa.ease.math;

/** A two-component vector. */
public record Vec2(float x, float y) {
  public static final Vec2 ZERO = new Vec2(0, 0);
  public static final Vec2 ONE = new Vec2(1, 1);

  public Vec2 add(Vec2 o) {
    return new Vec2(x + o.x, y + o.y);
  }

  public Vec2 sub(Vec2 o) {
    return new Vec2(x - o.x, y - o.y);
  }

  public Vec2 scale(float s) {
    return new Vec2(x * s, y * s);
  }

  public Vec2 negate() {
    return new Vec2(-x, -y);
  }

  public float dot(Vec2 o) {
    return x * o.x + y * o.y;
  }

  public float lengthSquared() {
    return dot(this);
  }

  public float length() {
    return (float) Math.sqrt(lengthSquared());
  }

  public float distance(Vec2 o) {
    return sub(o).length();
  }

  /** Returns this vector with length 1, or zero when its length is zero. */
  public Vec2 normalize() {
    float len = length();
    return len == 0 ? ZERO : scale(1 / len);
  }

  public Vec2 lerp(Vec2 o, float t) {
    return new Vec2(x + (o.x - x) * t, y + (o.y - y) * t);
  }

  public Vec2 min(Vec2 o) {
    return new Vec2(Math.min(x, o.x), Math.min(y, o.y));
  }

  public Vec2 max(Vec2 o) {
    return new Vec2(Math.max(x, o.x), Math.max(y, o.y));
  }

  /** True when every component differs from the other vector's by at most the tolerance. */
  public boolean approxEquals(Vec2 o, float tolerance) {
    return Math.abs(x - o.x) <= tolerance && Math.abs(y - o.y) <= tolerance;
  }

  public void writeTo(float[] dst, int offset) {
    dst[offset] = x;
    dst[offset + 1] = y;
  }

  public static Vec2 readFrom(float[] src, int offset) {
    return new Vec2(src[offset], src[offset + 1]);
  }
}

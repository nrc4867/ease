package dev.chieppa.ease.math;

/** A three-component vector. */
public record Vec3(float x, float y, float z) {
  public static final Vec3 ZERO = new Vec3(0, 0, 0);
  public static final Vec3 ONE = new Vec3(1, 1, 1);
  public static final Vec3 UNIT_X = new Vec3(1, 0, 0);
  public static final Vec3 UNIT_Y = new Vec3(0, 1, 0);
  public static final Vec3 UNIT_Z = new Vec3(0, 0, 1);

  public Vec3 add(Vec3 o) {
    return new Vec3(x + o.x, y + o.y, z + o.z);
  }

  public Vec3 sub(Vec3 o) {
    return new Vec3(x - o.x, y - o.y, z - o.z);
  }

  public Vec3 scale(float s) {
    return new Vec3(x * s, y * s, z * s);
  }

  public Vec3 negate() {
    return new Vec3(-x, -y, -z);
  }

  public float dot(Vec3 o) {
    return x * o.x + y * o.y + z * o.z;
  }

  public Vec3 cross(Vec3 o) {
    return new Vec3(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x);
  }

  public float lengthSquared() {
    return dot(this);
  }

  public float length() {
    return (float) Math.sqrt(lengthSquared());
  }

  public float distance(Vec3 o) {
    return sub(o).length();
  }

  /** Returns this vector with length 1, or zero when its length is zero. */
  public Vec3 normalize() {
    float len = length();
    return len == 0 ? ZERO : scale(1 / len);
  }

  public Vec3 lerp(Vec3 o, float t) {
    return new Vec3(x + (o.x - x) * t, y + (o.y - y) * t, z + (o.z - z) * t);
  }

  public Vec3 min(Vec3 o) {
    return new Vec3(Math.min(x, o.x), Math.min(y, o.y), Math.min(z, o.z));
  }

  public Vec3 max(Vec3 o) {
    return new Vec3(Math.max(x, o.x), Math.max(y, o.y), Math.max(z, o.z));
  }

  /** True when every component differs from the other vector's by at most the tolerance. */
  public boolean approxEquals(Vec3 o, float tolerance) {
    return Math.abs(x - o.x) <= tolerance
        && Math.abs(y - o.y) <= tolerance
        && Math.abs(z - o.z) <= tolerance;
  }

  public void writeTo(float[] dst, int offset) {
    Vec3s.set(dst, offset, x, y, z);
  }

  public static Vec3 readFrom(float[] src, int offset) {
    return new Vec3(src[offset], src[offset + 1], src[offset + 2]);
  }
}

package dev.chieppa.ease.math;

/** A four-component vector. */
public record Vec4(float x, float y, float z, float w) {
  public static final Vec4 ZERO = new Vec4(0, 0, 0, 0);
  public static final Vec4 ONE = new Vec4(1, 1, 1, 1);

  public Vec4 add(Vec4 o) {
    return new Vec4(x + o.x, y + o.y, z + o.z, w + o.w);
  }

  public Vec4 sub(Vec4 o) {
    return new Vec4(x - o.x, y - o.y, z - o.z, w - o.w);
  }

  public Vec4 scale(float s) {
    return new Vec4(x * s, y * s, z * s, w * s);
  }

  public Vec4 negate() {
    return new Vec4(-x, -y, -z, -w);
  }

  public float dot(Vec4 o) {
    return x * o.x + y * o.y + z * o.z + w * o.w;
  }

  public float lengthSquared() {
    return dot(this);
  }

  public float length() {
    return (float) Math.sqrt(lengthSquared());
  }

  public float distance(Vec4 o) {
    return sub(o).length();
  }

  /** Returns this vector with length 1, or zero when its length is zero. */
  public Vec4 normalize() {
    float len = length();
    return len == 0 ? ZERO : scale(1 / len);
  }

  public Vec4 lerp(Vec4 o, float t) {
    return new Vec4(x + (o.x - x) * t, y + (o.y - y) * t, z + (o.z - z) * t, w + (o.w - w) * t);
  }

  public Vec4 min(Vec4 o) {
    return new Vec4(Math.min(x, o.x), Math.min(y, o.y), Math.min(z, o.z), Math.min(w, o.w));
  }

  public Vec4 max(Vec4 o) {
    return new Vec4(Math.max(x, o.x), Math.max(y, o.y), Math.max(z, o.z), Math.max(w, o.w));
  }

  /** True when every component differs from the other vector's by at most the tolerance. */
  public boolean approxEquals(Vec4 o, float tolerance) {
    return Math.abs(x - o.x) <= tolerance
        && Math.abs(y - o.y) <= tolerance
        && Math.abs(z - o.z) <= tolerance
        && Math.abs(w - o.w) <= tolerance;
  }

  public void writeTo(float[] dst, int offset) {
    dst[offset] = x;
    dst[offset + 1] = y;
    dst[offset + 2] = z;
    dst[offset + 3] = w;
  }

  public static Vec4 readFrom(float[] src, int offset) {
    return new Vec4(src[offset], src[offset + 1], src[offset + 2], src[offset + 3]);
  }
}

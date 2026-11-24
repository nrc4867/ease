package dev.chieppa.ease.math;

/** Allocation-free {@link Vec3} operations on packed {@code float[]} data. */
public final class Vec3s {
  private Vec3s() {}

  public static void add(float[] a, int ao, float[] b, int bo, float[] dst, int d) {
    float x = a[ao] + b[bo];
    float y = a[ao + 1] + b[bo + 1];
    float z = a[ao + 2] + b[bo + 2];
    set(dst, d, x, y, z);
  }

  public static void sub(float[] a, int ao, float[] b, int bo, float[] dst, int d) {
    float x = a[ao] - b[bo];
    float y = a[ao + 1] - b[bo + 1];
    float z = a[ao + 2] - b[bo + 2];
    set(dst, d, x, y, z);
  }

  public static void scale(float[] a, int ao, float s, float[] dst, int d) {
    set(dst, d, a[ao] * s, a[ao + 1] * s, a[ao + 2] * s);
  }

  public static void negate(float[] a, int ao, float[] dst, int d) {
    set(dst, d, -a[ao], -a[ao + 1], -a[ao + 2]);
  }

  public static float dot(float[] a, int ao, float[] b, int bo) {
    return a[ao] * b[bo] + a[ao + 1] * b[bo + 1] + a[ao + 2] * b[bo + 2];
  }

  public static void cross(float[] a, int ao, float[] b, int bo, float[] dst, int d) {
    float x = a[ao + 1] * b[bo + 2] - a[ao + 2] * b[bo + 1];
    float y = a[ao + 2] * b[bo] - a[ao] * b[bo + 2];
    float z = a[ao] * b[bo + 1] - a[ao + 1] * b[bo];
    set(dst, d, x, y, z);
  }

  public static float lengthSquared(float[] a, int ao) {
    return dot(a, ao, a, ao);
  }

  public static float length(float[] a, int ao) {
    return (float) Math.sqrt(lengthSquared(a, ao));
  }

  public static float distance(float[] a, int ao, float[] b, int bo) {
    float dx = a[ao] - b[bo];
    float dy = a[ao + 1] - b[bo + 1];
    float dz = a[ao + 2] - b[bo + 2];
    return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
  }

  /** Writes the vector scaled to length 1, or zero when its length is zero. */
  public static void normalize(float[] a, int ao, float[] dst, int d) {
    float len = length(a, ao);
    float inv = len == 0 ? 0 : 1 / len;
    scale(a, ao, inv, dst, d);
  }

  public static void lerp(float[] a, int ao, float[] b, int bo, float t, float[] dst, int d) {
    float x = a[ao] + (b[bo] - a[ao]) * t;
    float y = a[ao + 1] + (b[bo + 1] - a[ao + 1]) * t;
    float z = a[ao + 2] + (b[bo + 2] - a[ao + 2]) * t;
    set(dst, d, x, y, z);
  }

  public static void min(float[] a, int ao, float[] b, int bo, float[] dst, int d) {
    float x = Math.min(a[ao], b[bo]);
    float y = Math.min(a[ao + 1], b[bo + 1]);
    float z = Math.min(a[ao + 2], b[bo + 2]);
    set(dst, d, x, y, z);
  }

  public static void max(float[] a, int ao, float[] b, int bo, float[] dst, int d) {
    float x = Math.max(a[ao], b[bo]);
    float y = Math.max(a[ao + 1], b[bo + 1]);
    float z = Math.max(a[ao + 2], b[bo + 2]);
    set(dst, d, x, y, z);
  }

  /** Writes the three components at {@code d}. */
  public static void set(float[] dst, int d, float x, float y, float z) {
    dst[d] = x;
    dst[d + 1] = y;
    dst[d + 2] = z;
  }
}

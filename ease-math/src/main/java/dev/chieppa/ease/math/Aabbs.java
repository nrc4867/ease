package dev.chieppa.ease.math;

/**
 * Allocation-free bounds operations on packed {@code float[6]} boxes: min x, y, z then max x, y, z.
 */
public final class Aabbs {
  private Aabbs() {}

  /** Writes the box around {@code count} packed points; an empty set gives an inverted box. */
  public static void ofPoints(float[] points, int po, int count, float[] dst, int d) {
    Vec3s.set(dst, d, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
    Vec3s.set(
        dst, d + 3, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY);
    for (int i = 0; i < count; i++) {
      int p = po + i * 3;
      Vec3s.min(dst, d, points, p, dst, d);
      Vec3s.max(dst, d + 3, points, p, dst, d + 3);
    }
  }

  public static void union(float[] a, int ao, float[] b, int bo, float[] dst, int d) {
    Vec3s.min(a, ao, b, bo, dst, d);
    Vec3s.max(a, ao + 3, b, bo + 3, dst, d + 3);
  }

  public static boolean containsPoint(float[] box, int bo, float[] p, int po) {
    for (int i = 0; i < 3; i++) {
      if (p[po + i] < box[bo + i] || p[po + i] > box[bo + 3 + i]) {
        return false;
      }
    }
    return true;
  }

  /** True when the boxes overlap or touch. */
  public static boolean intersects(float[] a, int ao, float[] b, int bo) {
    for (int i = 0; i < 3; i++) {
      if (a[ao + i] > b[bo + 3 + i] || b[bo + i] > a[ao + 3 + i]) {
        return false;
      }
    }
    return true;
  }

  /**
   * Writes the box around the transformed box (Arvo's method): the transformed center, with each
   * half-extent summed from the absolute values of the linear part.
   */
  public static void transform(float[] box, int bo, float[] m, int mo, float[] dst, int d) {
    float cx = (box[bo] + box[bo + 3]) * 0.5f;
    float cy = (box[bo + 1] + box[bo + 4]) * 0.5f;
    float cz = (box[bo + 2] + box[bo + 5]) * 0.5f;
    float ex = (box[bo + 3] - box[bo]) * 0.5f;
    float ey = (box[bo + 4] - box[bo + 1]) * 0.5f;
    float ez = (box[bo + 5] - box[bo + 2]) * 0.5f;
    for (int row = 0; row < 3; row++) {
      float center = m[mo + row] * cx + m[mo + 4 + row] * cy + m[mo + 8 + row] * cz;
      center += m[mo + 12 + row];
      float extent = Math.abs(m[mo + row]) * ex;
      extent += Math.abs(m[mo + 4 + row]) * ey;
      extent += Math.abs(m[mo + 8 + row]) * ez;
      dst[d + row] = center - extent;
      dst[d + 3 + row] = center + extent;
    }
  }
}

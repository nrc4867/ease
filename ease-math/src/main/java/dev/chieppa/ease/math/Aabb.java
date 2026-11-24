package dev.chieppa.ease.math;

/** An axis-aligned box from {@code min} to {@code max}, inclusive. */
public record Aabb(Vec3 min, Vec3 max) {
  /** The smallest box containing every point; the points must not be empty. */
  public static Aabb of(Vec3... points) {
    if (points.length == 0) {
      throw new IllegalArgumentException("A box needs at least one point");
    }
    Vec3 low = points[0];
    Vec3 high = points[0];
    for (Vec3 p : points) {
      low = low.min(p);
      high = high.max(p);
    }
    return new Aabb(low, high);
  }

  public Aabb union(Aabb o) {
    return new Aabb(min.min(o.min), max.max(o.max));
  }

  public boolean contains(Vec3 p) {
    return p.x() >= min.x()
        && p.x() <= max.x()
        && p.y() >= min.y()
        && p.y() <= max.y()
        && p.z() >= min.z()
        && p.z() <= max.z();
  }

  /** True when the boxes overlap or touch. */
  public boolean intersects(Aabb o) {
    return min.x() <= o.max.x()
        && o.min.x() <= max.x()
        && min.y() <= o.max.y()
        && o.min.y() <= max.y()
        && min.z() <= o.max.z()
        && o.min.z() <= max.z();
  }

  public Vec3 center() {
    return min.add(max).scale(0.5f);
  }

  /** Half the size along each axis. */
  public Vec3 extents() {
    return max.sub(min).scale(0.5f);
  }

  /**
   * The box around this box after the affine part of {@code m} is applied (Arvo's method): the
   * transformed center, with each half-extent summed from the absolute values of the linear part.
   */
  public Aabb transformed(Mat4 m) {
    Vec3 c = m.transformPoint(center());
    Vec3 e = extents();
    float ex = Math.abs(m.m00()) * e.x() + Math.abs(m.m10()) * e.y() + Math.abs(m.m20()) * e.z();
    float ey = Math.abs(m.m01()) * e.x() + Math.abs(m.m11()) * e.y() + Math.abs(m.m21()) * e.z();
    float ez = Math.abs(m.m02()) * e.x() + Math.abs(m.m12()) * e.y() + Math.abs(m.m22()) * e.z();
    Vec3 extent = new Vec3(ex, ey, ez);
    return new Aabb(c.sub(extent), c.add(extent));
  }

  public void writeTo(float[] dst, int offset) {
    min.writeTo(dst, offset);
    max.writeTo(dst, offset + 3);
  }

  public static Aabb readFrom(float[] src, int offset) {
    return new Aabb(Vec3.readFrom(src, offset), Vec3.readFrom(src, offset + 3));
  }
}

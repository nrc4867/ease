package dev.chieppa.ease.math;

/**
 * A 4x4 matrix with components named {@code mCR} for column C, row R, in column-major order, so
 * {@link #writeTo} produces the layout WGSL expects.
 */
public record Mat4(
    float m00,
    float m01,
    float m02,
    float m03,
    float m10,
    float m11,
    float m12,
    float m13,
    float m20,
    float m21,
    float m22,
    float m23,
    float m30,
    float m31,
    float m32,
    float m33) {
  public static final Mat4 IDENTITY = scaling(Vec3.ONE);

  public static Mat4 translation(Vec3 t) {
    return new Mat4(1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, t.x(), t.y(), t.z(), 1);
  }

  public static Mat4 scaling(Vec3 s) {
    return new Mat4(s.x(), 0, 0, 0, 0, s.y(), 0, 0, 0, 0, s.z(), 0, 0, 0, 0, 1);
  }

  public static Mat4 rotation(Quat q) {
    return Mat4Factory.fromTransform(Vec3.ZERO, q, Vec3.ONE);
  }

  /** The matrix translation * rotation * scale. */
  public static Mat4 fromTransform(Vec3 t, Quat q, Vec3 s) {
    return Mat4Factory.fromTransform(t, q, s);
  }

  /** A perspective projection looking down -Z, with depth 0 at near and 1 at far. */
  public static Mat4 perspective(float fovY, float aspect, float near, float far) {
    return Mat4Factory.perspective(fovY, aspect, near, far);
  }

  /** An orthographic projection looking down -Z, with depth 0 at near and 1 at far. */
  public static Mat4 orthographic(
      float left, float right, float bottom, float top, float near, float far) {
    return Mat4Factory.orthographic(left, right, bottom, top, near, far);
  }

  /** The view matrix of an eye at {@code eye} looking at {@code target}. */
  public static Mat4 lookAt(Vec3 eye, Vec3 target, Vec3 up) {
    return Mat4Factory.lookAt(eye, target, up);
  }

  /** The transform that applies {@code b} first, then this one. */
  public Mat4 mul(Mat4 b) {
    float r00 = m00 * b.m00 + m10 * b.m01 + m20 * b.m02 + m30 * b.m03;
    float r01 = m01 * b.m00 + m11 * b.m01 + m21 * b.m02 + m31 * b.m03;
    float r02 = m02 * b.m00 + m12 * b.m01 + m22 * b.m02 + m32 * b.m03;
    float r03 = m03 * b.m00 + m13 * b.m01 + m23 * b.m02 + m33 * b.m03;
    float r10 = m00 * b.m10 + m10 * b.m11 + m20 * b.m12 + m30 * b.m13;
    float r11 = m01 * b.m10 + m11 * b.m11 + m21 * b.m12 + m31 * b.m13;
    float r12 = m02 * b.m10 + m12 * b.m11 + m22 * b.m12 + m32 * b.m13;
    float r13 = m03 * b.m10 + m13 * b.m11 + m23 * b.m12 + m33 * b.m13;
    float r20 = m00 * b.m20 + m10 * b.m21 + m20 * b.m22 + m30 * b.m23;
    float r21 = m01 * b.m20 + m11 * b.m21 + m21 * b.m22 + m31 * b.m23;
    float r22 = m02 * b.m20 + m12 * b.m21 + m22 * b.m22 + m32 * b.m23;
    float r23 = m03 * b.m20 + m13 * b.m21 + m23 * b.m22 + m33 * b.m23;
    float r30 = m00 * b.m30 + m10 * b.m31 + m20 * b.m32 + m30 * b.m33;
    float r31 = m01 * b.m30 + m11 * b.m31 + m21 * b.m32 + m31 * b.m33;
    float r32 = m02 * b.m30 + m12 * b.m31 + m22 * b.m32 + m32 * b.m33;
    float r33 = m03 * b.m30 + m13 * b.m31 + m23 * b.m32 + m33 * b.m33;
    return new Mat4(r00, r01, r02, r03, r10, r11, r12, r13, r20, r21, r22, r23, r30, r31, r32, r33);
  }

  public Mat4 transpose() {
    return new Mat4(m00, m10, m20, m30, m01, m11, m21, m31, m02, m12, m22, m32, m03, m13, m23, m33);
  }

  public float determinant() {
    return Mat4Inversion.determinant(this);
  }

  /** The inverse of a general matrix; throws when it is singular. */
  public Mat4 inverse() {
    return requireInvertible(Mat4Inversion.inverse(this));
  }

  /** The inverse of a matrix whose last row is 0, 0, 0, 1; throws when it is singular. */
  public Mat4 inverseAffine() {
    return requireInvertible(Mat4Inversion.inverseAffine(this));
  }

  /** The point transformed by the affine part of the matrix (w taken as 1, no divide). */
  public Vec3 transformPoint(Vec3 p) {
    float x = m00 * p.x() + m10 * p.y() + m20 * p.z() + m30;
    float y = m01 * p.x() + m11 * p.y() + m21 * p.z() + m31;
    float z = m02 * p.x() + m12 * p.y() + m22 * p.z() + m32;
    return new Vec3(x, y, z);
  }

  /** The direction transformed by the linear part of the matrix (translation ignored). */
  public Vec3 transformDirection(Vec3 v) {
    float x = m00 * v.x() + m10 * v.y() + m20 * v.z();
    float y = m01 * v.x() + m11 * v.y() + m21 * v.z();
    float z = m02 * v.x() + m12 * v.y() + m22 * v.z();
    return new Vec3(x, y, z);
  }

  public Vec4 transform(Vec4 v) {
    float x = m00 * v.x() + m10 * v.y() + m20 * v.z() + m30 * v.w();
    float y = m01 * v.x() + m11 * v.y() + m21 * v.z() + m31 * v.w();
    float z = m02 * v.x() + m12 * v.y() + m22 * v.z() + m32 * v.w();
    float w = m03 * v.x() + m13 * v.y() + m23 * v.z() + m33 * v.w();
    return new Vec4(x, y, z, w);
  }

  /** True when every element differs from the other matrix's by at most the tolerance. */
  public boolean approxEquals(Mat4 o, float tolerance) {
    return Math.abs(m00 - o.m00) <= tolerance
        && Math.abs(m01 - o.m01) <= tolerance
        && Math.abs(m02 - o.m02) <= tolerance
        && Math.abs(m03 - o.m03) <= tolerance
        && Math.abs(m10 - o.m10) <= tolerance
        && Math.abs(m11 - o.m11) <= tolerance
        && Math.abs(m12 - o.m12) <= tolerance
        && Math.abs(m13 - o.m13) <= tolerance
        && Math.abs(m20 - o.m20) <= tolerance
        && Math.abs(m21 - o.m21) <= tolerance
        && Math.abs(m22 - o.m22) <= tolerance
        && Math.abs(m23 - o.m23) <= tolerance
        && Math.abs(m30 - o.m30) <= tolerance
        && Math.abs(m31 - o.m31) <= tolerance
        && Math.abs(m32 - o.m32) <= tolerance
        && Math.abs(m33 - o.m33) <= tolerance;
  }

  public void writeTo(float[] dst, int offset) {
    dst[offset] = m00;
    dst[offset + 1] = m01;
    dst[offset + 2] = m02;
    dst[offset + 3] = m03;
    dst[offset + 4] = m10;
    dst[offset + 5] = m11;
    dst[offset + 6] = m12;
    dst[offset + 7] = m13;
    dst[offset + 8] = m20;
    dst[offset + 9] = m21;
    dst[offset + 10] = m22;
    dst[offset + 11] = m23;
    dst[offset + 12] = m30;
    dst[offset + 13] = m31;
    dst[offset + 14] = m32;
    dst[offset + 15] = m33;
  }

  public static Mat4 readFrom(float[] s, int o) {
    return new Mat4(
        s[o], s[o + 1], s[o + 2], s[o + 3], s[o + 4], s[o + 5], s[o + 6], s[o + 7], s[o + 8],
        s[o + 9], s[o + 10], s[o + 11], s[o + 12], s[o + 13], s[o + 14], s[o + 15]);
  }

  private static Mat4 requireInvertible(Mat4 inverse) {
    if (inverse == null) {
      throw new ArithmeticException("Matrix is singular");
    }
    return inverse;
  }
}

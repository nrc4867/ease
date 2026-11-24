package dev.chieppa.ease.math;

/** Allocation-free 4x4 matrix operations on packed {@code float[16]} data, column-major. */
public final class Mat4s {
  private static final int SIZE = 16;

  private Mat4s() {}

  public static void identity(float[] dst, int d) {
    translation(0, 0, 0, dst, d);
  }

  public static void translation(float x, float y, float z, float[] dst, int d) {
    write(dst, d, x, y, z, 0, 0, 0, 1, 1, 1, 1);
  }

  public static void scaling(float x, float y, float z, float[] dst, int d) {
    write(dst, d, 0, 0, 0, 0, 0, 0, 1, x, y, z);
  }

  /** Writes the rotation of the unit quaternion at {@code qo}. */
  public static void rotation(float[] q, int qo, float[] dst, int d) {
    write(dst, d, 0, 0, 0, q[qo], q[qo + 1], q[qo + 2], q[qo + 3], 1, 1, 1);
  }

  /** Writes translation * rotation * scale from three packed components. */
  public static void fromTransform(
      float[] t, int to, float[] q, int qo, float[] s, int so, float[] dst, int d) {
    write(
        dst, d, t[to], t[to + 1], t[to + 2], q[qo], q[qo + 1], q[qo + 2], q[qo + 3], s[so],
        s[so + 1], s[so + 2]);
  }

  /** Writes {@code a * b}: the transform that applies {@code b} first, then {@code a}. */
  public static void mul(float[] a, int ao, float[] b, int bo, float[] dst, int d) {
    float a00 = a[ao];
    float a01 = a[ao + 1];
    float a02 = a[ao + 2];
    float a03 = a[ao + 3];
    float a10 = a[ao + 4];
    float a11 = a[ao + 5];
    float a12 = a[ao + 6];
    float a13 = a[ao + 7];
    float a20 = a[ao + 8];
    float a21 = a[ao + 9];
    float a22 = a[ao + 10];
    float a23 = a[ao + 11];
    float a30 = a[ao + 12];
    float a31 = a[ao + 13];
    float a32 = a[ao + 14];
    float a33 = a[ao + 15];
    for (int col = 0; col < 4; col++) {
      int bi = bo + col * 4;
      float b0 = b[bi];
      float b1 = b[bi + 1];
      float b2 = b[bi + 2];
      float b3 = b[bi + 3];
      int di = d + col * 4;
      dst[di] = a00 * b0 + a10 * b1 + a20 * b2 + a30 * b3;
      dst[di + 1] = a01 * b0 + a11 * b1 + a21 * b2 + a31 * b3;
      dst[di + 2] = a02 * b0 + a12 * b1 + a22 * b2 + a32 * b3;
      dst[di + 3] = a03 * b0 + a13 * b1 + a23 * b2 + a33 * b3;
    }
  }

  public static void transpose(float[] a, int ao, float[] dst, int d) {
    System.arraycopy(a, ao, dst, d, SIZE);
    for (int col = 0; col < 4; col++) {
      for (int row = col + 1; row < 4; row++) {
        float upper = dst[d + col * 4 + row];
        dst[d + col * 4 + row] = dst[d + row * 4 + col];
        dst[d + row * 4 + col] = upper;
      }
    }
  }

  /** Writes the point transformed by the affine part of the matrix (w taken as 1, no divide). */
  public static void transformPoint(float[] m, int mo, float[] p, int po, float[] dst, int d) {
    float x = p[po];
    float y = p[po + 1];
    float z = p[po + 2];
    float rx = m[mo] * x + m[mo + 4] * y + m[mo + 8] * z + m[mo + 12];
    float ry = m[mo + 1] * x + m[mo + 5] * y + m[mo + 9] * z + m[mo + 13];
    float rz = m[mo + 2] * x + m[mo + 6] * y + m[mo + 10] * z + m[mo + 14];
    Vec3s.set(dst, d, rx, ry, rz);
  }

  /** Writes the direction transformed by the linear part of the matrix (translation ignored). */
  public static void transformDirection(float[] m, int mo, float[] v, int vo, float[] dst, int d) {
    float x = v[vo];
    float y = v[vo + 1];
    float z = v[vo + 2];
    float rx = m[mo] * x + m[mo + 4] * y + m[mo + 8] * z;
    float ry = m[mo + 1] * x + m[mo + 5] * y + m[mo + 9] * z;
    float rz = m[mo + 2] * x + m[mo + 6] * y + m[mo + 10] * z;
    Vec3s.set(dst, d, rx, ry, rz);
  }

  /** Writes the four-component vector at {@code vo} multiplied by the matrix. */
  public static void transformVec4(float[] m, int mo, float[] v, int vo, float[] dst, int d) {
    float x = v[vo];
    float y = v[vo + 1];
    float z = v[vo + 2];
    float w = v[vo + 3];
    for (int row = 0; row < 4; row++) {
      float value = m[mo + row] * x + m[mo + 4 + row] * y + m[mo + 8 + row] * z;
      dst[d + row] = value + m[mo + 12 + row] * w;
    }
  }

  private static void write(
      float[] dst,
      int d,
      float tx,
      float ty,
      float tz,
      float qx,
      float qy,
      float qz,
      float qw,
      float sx,
      float sy,
      float sz) {
    float xx = qx * qx;
    float yy = qy * qy;
    float zz = qz * qz;
    float xy = qx * qy;
    float xz = qx * qz;
    float yz = qy * qz;
    float wx = qw * qx;
    float wy = qw * qy;
    float wz = qw * qz;
    dst[d] = (1 - 2 * (yy + zz)) * sx;
    dst[d + 1] = 2 * (xy + wz) * sx;
    dst[d + 2] = 2 * (xz - wy) * sx;
    dst[d + 3] = 0;
    dst[d + 4] = 2 * (xy - wz) * sy;
    dst[d + 5] = (1 - 2 * (xx + zz)) * sy;
    dst[d + 6] = 2 * (yz + wx) * sy;
    dst[d + 7] = 0;
    dst[d + 8] = 2 * (xz + wy) * sz;
    dst[d + 9] = 2 * (yz - wx) * sz;
    dst[d + 10] = (1 - 2 * (xx + yy)) * sz;
    dst[d + 11] = 0;
    dst[d + 12] = tx;
    dst[d + 13] = ty;
    dst[d + 14] = tz;
    dst[d + 15] = 1;
  }
}

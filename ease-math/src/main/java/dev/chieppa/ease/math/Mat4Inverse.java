package dev.chieppa.ease.math;

/** Allocation-free determinant and inverse of packed column-major {@code float[16]} matrices. */
public final class Mat4Inverse {
  private Mat4Inverse() {}

  /**
   * Writes the inverse of a general matrix and returns true, or returns false and leaves {@code
   * dst} untouched when the matrix is singular.
   */
  public static boolean inverse(float[] m, int mo, float[] dst, int d) {
    float a00 = m[mo];
    float a01 = m[mo + 1];
    float a02 = m[mo + 2];
    float a03 = m[mo + 3];
    float a10 = m[mo + 4];
    float a11 = m[mo + 5];
    float a12 = m[mo + 6];
    float a13 = m[mo + 7];
    float a20 = m[mo + 8];
    float a21 = m[mo + 9];
    float a22 = m[mo + 10];
    float a23 = m[mo + 11];
    float a30 = m[mo + 12];
    float a31 = m[mo + 13];
    float a32 = m[mo + 14];
    float a33 = m[mo + 15];
    float s0 = a00 * a11 - a10 * a01;
    float s1 = a00 * a12 - a10 * a02;
    float s2 = a00 * a13 - a10 * a03;
    float s3 = a01 * a12 - a11 * a02;
    float s4 = a01 * a13 - a11 * a03;
    float s5 = a02 * a13 - a12 * a03;
    float c5 = a22 * a33 - a32 * a23;
    float c4 = a21 * a33 - a31 * a23;
    float c3 = a21 * a32 - a31 * a22;
    float c2 = a20 * a33 - a30 * a23;
    float c1 = a20 * a32 - a30 * a22;
    float c0 = a20 * a31 - a30 * a21;
    float det = s0 * c5 - s1 * c4 + s2 * c3 + s3 * c2 - s4 * c1 + s5 * c0;
    if (det == 0 || Float.isNaN(det)) {
      return false;
    }
    float inv = 1 / det;
    dst[d] = (a11 * c5 - a12 * c4 + a13 * c3) * inv;
    dst[d + 1] = (-a01 * c5 + a02 * c4 - a03 * c3) * inv;
    dst[d + 2] = (a31 * s5 - a32 * s4 + a33 * s3) * inv;
    dst[d + 3] = (-a21 * s5 + a22 * s4 - a23 * s3) * inv;
    dst[d + 4] = (-a10 * c5 + a12 * c2 - a13 * c1) * inv;
    dst[d + 5] = (a00 * c5 - a02 * c2 + a03 * c1) * inv;
    dst[d + 6] = (-a30 * s5 + a32 * s2 - a33 * s1) * inv;
    dst[d + 7] = (a20 * s5 - a22 * s2 + a23 * s1) * inv;
    dst[d + 8] = (a10 * c4 - a11 * c2 + a13 * c0) * inv;
    dst[d + 9] = (-a00 * c4 + a01 * c2 - a03 * c0) * inv;
    dst[d + 10] = (a30 * s4 - a31 * s2 + a33 * s0) * inv;
    dst[d + 11] = (-a20 * s4 + a21 * s2 - a23 * s0) * inv;
    dst[d + 12] = (-a10 * c3 + a11 * c1 - a12 * c0) * inv;
    dst[d + 13] = (a00 * c3 - a01 * c1 + a02 * c0) * inv;
    dst[d + 14] = (-a30 * s3 + a31 * s1 - a32 * s0) * inv;
    dst[d + 15] = (a20 * s3 - a21 * s1 + a22 * s0) * inv;
    return true;
  }

  public static float determinant(float[] m, int mo) {
    float a00 = m[mo];
    float a01 = m[mo + 1];
    float a02 = m[mo + 2];
    float a03 = m[mo + 3];
    float a10 = m[mo + 4];
    float a11 = m[mo + 5];
    float a12 = m[mo + 6];
    float a13 = m[mo + 7];
    float s0 = a00 * a11 - a10 * a01;
    float s1 = a00 * a12 - a10 * a02;
    float s2 = a00 * a13 - a10 * a03;
    float s3 = a01 * a12 - a11 * a02;
    float s4 = a01 * a13 - a11 * a03;
    float s5 = a02 * a13 - a12 * a03;
    float a20 = m[mo + 8];
    float a21 = m[mo + 9];
    float a22 = m[mo + 10];
    float a23 = m[mo + 11];
    float a30 = m[mo + 12];
    float a31 = m[mo + 13];
    float a32 = m[mo + 14];
    float a33 = m[mo + 15];
    float c5 = a22 * a33 - a32 * a23;
    float c4 = a21 * a33 - a31 * a23;
    float c3 = a21 * a32 - a31 * a22;
    float c2 = a20 * a33 - a30 * a23;
    float c1 = a20 * a32 - a30 * a22;
    float c0 = a20 * a31 - a30 * a21;
    return s0 * c5 - s1 * c4 + s2 * c3 + s3 * c2 - s4 * c1 + s5 * c0;
  }

  /**
   * Writes the inverse of a matrix whose last row is 0, 0, 0, 1 and returns true, or returns false
   * and leaves {@code dst} untouched when its linear part is singular.
   */
  public static boolean inverseAffine(float[] m, int mo, float[] dst, int d) {
    float c0x = m[mo];
    float c0y = m[mo + 1];
    float c0z = m[mo + 2];
    float c1x = m[mo + 4];
    float c1y = m[mo + 5];
    float c1z = m[mo + 6];
    float c2x = m[mo + 8];
    float c2y = m[mo + 9];
    float c2z = m[mo + 10];
    float tx = m[mo + 12];
    float ty = m[mo + 13];
    float tz = m[mo + 14];
    // Rows of the inverse linear part are the cross products of the other two columns.
    float r0x = c1y * c2z - c1z * c2y;
    float r0y = c1z * c2x - c1x * c2z;
    float r0z = c1x * c2y - c1y * c2x;
    float det = c0x * r0x + c0y * r0y + c0z * r0z;
    if (det == 0 || Float.isNaN(det)) {
      return false;
    }
    float inv = 1 / det;
    float r1x = c2y * c0z - c2z * c0y;
    float r1y = c2z * c0x - c2x * c0z;
    float r1z = c2x * c0y - c2y * c0x;
    float r2x = c0y * c1z - c0z * c1y;
    float r2y = c0z * c1x - c0x * c1z;
    float r2z = c0x * c1y - c0y * c1x;
    dst[d] = r0x * inv;
    dst[d + 1] = r1x * inv;
    dst[d + 2] = r2x * inv;
    dst[d + 3] = 0;
    dst[d + 4] = r0y * inv;
    dst[d + 5] = r1y * inv;
    dst[d + 6] = r2y * inv;
    dst[d + 7] = 0;
    dst[d + 8] = r0z * inv;
    dst[d + 9] = r1z * inv;
    dst[d + 10] = r2z * inv;
    dst[d + 11] = 0;
    dst[d + 12] = -(r0x * tx + r0y * ty + r0z * tz) * inv;
    dst[d + 13] = -(r1x * tx + r1y * ty + r1z * tz) * inv;
    dst[d + 14] = -(r2x * tx + r2y * ty + r2z * tz) * inv;
    dst[d + 15] = 1;
    return true;
  }
}

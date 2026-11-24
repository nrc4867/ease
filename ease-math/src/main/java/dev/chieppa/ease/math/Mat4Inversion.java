package dev.chieppa.ease.math;

/** Determinant and inverses of {@link Mat4}, computed on its components. */
final class Mat4Inversion {
  private Mat4Inversion() {}

  /** Returns the inverse of a general matrix, or null when it is singular. */
  static Mat4 inverse(Mat4 m) {
    float a00 = m.m00();
    float a01 = m.m01();
    float a02 = m.m02();
    float a03 = m.m03();
    float a10 = m.m10();
    float a11 = m.m11();
    float a12 = m.m12();
    float a13 = m.m13();
    float a20 = m.m20();
    float a21 = m.m21();
    float a22 = m.m22();
    float a23 = m.m23();
    float a30 = m.m30();
    float a31 = m.m31();
    float a32 = m.m32();
    float a33 = m.m33();
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
      return null;
    }
    float inv = 1 / det;
    float r0 = (a11 * c5 - a12 * c4 + a13 * c3) * inv;
    float r1 = (-a01 * c5 + a02 * c4 - a03 * c3) * inv;
    float r2 = (a31 * s5 - a32 * s4 + a33 * s3) * inv;
    float r3 = (-a21 * s5 + a22 * s4 - a23 * s3) * inv;
    float r4 = (-a10 * c5 + a12 * c2 - a13 * c1) * inv;
    float r5 = (a00 * c5 - a02 * c2 + a03 * c1) * inv;
    float r6 = (-a30 * s5 + a32 * s2 - a33 * s1) * inv;
    float r7 = (a20 * s5 - a22 * s2 + a23 * s1) * inv;
    float r8 = (a10 * c4 - a11 * c2 + a13 * c0) * inv;
    float r9 = (-a00 * c4 + a01 * c2 - a03 * c0) * inv;
    float r10 = (a30 * s4 - a31 * s2 + a33 * s0) * inv;
    float r11 = (-a20 * s4 + a21 * s2 - a23 * s0) * inv;
    float r12 = (-a10 * c3 + a11 * c1 - a12 * c0) * inv;
    float r13 = (a00 * c3 - a01 * c1 + a02 * c0) * inv;
    float r14 = (-a30 * s3 + a31 * s1 - a32 * s0) * inv;
    float r15 = (a20 * s3 - a21 * s1 + a22 * s0) * inv;
    return new Mat4(r0, r1, r2, r3, r4, r5, r6, r7, r8, r9, r10, r11, r12, r13, r14, r15);
  }

  static float determinant(Mat4 m) {
    float a00 = m.m00();
    float a01 = m.m01();
    float a02 = m.m02();
    float a03 = m.m03();
    float a10 = m.m10();
    float a11 = m.m11();
    float a12 = m.m12();
    float a13 = m.m13();
    float s0 = a00 * a11 - a10 * a01;
    float s1 = a00 * a12 - a10 * a02;
    float s2 = a00 * a13 - a10 * a03;
    float s3 = a01 * a12 - a11 * a02;
    float s4 = a01 * a13 - a11 * a03;
    float s5 = a02 * a13 - a12 * a03;
    float a20 = m.m20();
    float a21 = m.m21();
    float a22 = m.m22();
    float a23 = m.m23();
    float a30 = m.m30();
    float a31 = m.m31();
    float a32 = m.m32();
    float a33 = m.m33();
    float c5 = a22 * a33 - a32 * a23;
    float c4 = a21 * a33 - a31 * a23;
    float c3 = a21 * a32 - a31 * a22;
    float c2 = a20 * a33 - a30 * a23;
    float c1 = a20 * a32 - a30 * a22;
    float c0 = a20 * a31 - a30 * a21;
    return s0 * c5 - s1 * c4 + s2 * c3 + s3 * c2 - s4 * c1 + s5 * c0;
  }

  /** Returns the inverse of a matrix whose last row is 0, 0, 0, 1, or null when singular. */
  static Mat4 inverseAffine(Mat4 m) {
    float c0x = m.m00();
    float c0y = m.m01();
    float c0z = m.m02();
    float c1x = m.m10();
    float c1y = m.m11();
    float c1z = m.m12();
    float c2x = m.m20();
    float c2y = m.m21();
    float c2z = m.m22();
    float tx = m.m30();
    float ty = m.m31();
    float tz = m.m32();
    // Rows of the inverse linear part are the cross products of the other two columns.
    float r0x = c1y * c2z - c1z * c2y;
    float r0y = c1z * c2x - c1x * c2z;
    float r0z = c1x * c2y - c1y * c2x;
    float det = c0x * r0x + c0y * r0y + c0z * r0z;
    if (det == 0 || Float.isNaN(det)) {
      return null;
    }
    float inv = 1 / det;
    float r1x = c2y * c0z - c2z * c0y;
    float r1y = c2z * c0x - c2x * c0z;
    float r1z = c2x * c0y - c2y * c0x;
    float r2x = c0y * c1z - c0z * c1y;
    float r2y = c0z * c1x - c0x * c1z;
    float r2z = c0x * c1y - c0y * c1x;
    float r0 = r0x * inv;
    float r1 = r1x * inv;
    float r2 = r2x * inv;
    float r3 = 0;
    float r4 = r0y * inv;
    float r5 = r1y * inv;
    float r6 = r2y * inv;
    float r7 = 0;
    float r8 = r0z * inv;
    float r9 = r1z * inv;
    float r10 = r2z * inv;
    float r11 = 0;
    float r12 = -(r0x * tx + r0y * ty + r0z * tz) * inv;
    float r13 = -(r1x * tx + r1y * ty + r1z * tz) * inv;
    float r14 = -(r2x * tx + r2y * ty + r2z * tz) * inv;
    float r15 = 1;
    return new Mat4(r0, r1, r2, r3, r4, r5, r6, r7, r8, r9, r10, r11, r12, r13, r14, r15);
  }
}

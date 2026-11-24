package dev.chieppa.ease.math;

/** Allocation-free view and projection matrices, written column-major into {@code float[16]}. */
public final class Mat4Cameras {
  private Mat4Cameras() {}

  /** Writes a perspective projection looking down -Z, mapping depth from 0 at near to 1 at far. */
  public static void perspective(
      float fovY, float aspect, float near, float far, float[] dst, int d) {
    float f = 1 / (float) Math.tan(fovY * 0.5f);
    float range = near - far;
    java.util.Arrays.fill(dst, d, d + 16, 0f);
    dst[d] = f / aspect;
    dst[d + 5] = f;
    dst[d + 10] = far / range;
    dst[d + 11] = -1;
    dst[d + 14] = near * far / range;
  }

  /** Writes an orthographic projection looking down -Z, mapping depth from 0 to 1. */
  public static void orthographic(
      float left, float right, float bottom, float top, float near, float far, float[] dst, int d) {
    float range = near - far;
    java.util.Arrays.fill(dst, d, d + 16, 0f);
    dst[d] = 2 / (right - left);
    dst[d + 5] = 2 / (top - bottom);
    dst[d + 10] = 1 / range;
    dst[d + 12] = -(right + left) / (right - left);
    dst[d + 13] = -(top + bottom) / (top - bottom);
    dst[d + 14] = near / range;
    dst[d + 15] = 1;
  }

  /** Writes the view matrix of an eye at {@code eye} looking at {@code target}. */
  public static void lookAt(
      float[] eye, int eo, float[] target, int to, float[] up, int uo, float[] dst, int d) {
    float fx = target[to] - eye[eo];
    float fy = target[to + 1] - eye[eo + 1];
    float fz = target[to + 2] - eye[eo + 2];
    float fInv = 1 / (float) Math.sqrt(fx * fx + fy * fy + fz * fz);
    fx *= fInv;
    fy *= fInv;
    fz *= fInv;
    float sx = fy * up[uo + 2] - fz * up[uo + 1];
    float sy = fz * up[uo] - fx * up[uo + 2];
    float sz = fx * up[uo + 1] - fy * up[uo];
    float sInv = 1 / (float) Math.sqrt(sx * sx + sy * sy + sz * sz);
    sx *= sInv;
    sy *= sInv;
    sz *= sInv;
    float ux = sy * fz - sz * fy;
    float uy = sz * fx - sx * fz;
    float uz = sx * fy - sy * fx;
    float ex = eye[eo];
    float ey = eye[eo + 1];
    float ez = eye[eo + 2];
    dst[d] = sx;
    dst[d + 1] = ux;
    dst[d + 2] = -fx;
    dst[d + 3] = 0;
    dst[d + 4] = sy;
    dst[d + 5] = uy;
    dst[d + 6] = -fy;
    dst[d + 7] = 0;
    dst[d + 8] = sz;
    dst[d + 9] = uz;
    dst[d + 10] = -fz;
    dst[d + 11] = 0;
    dst[d + 12] = -(sx * ex + sy * ey + sz * ez);
    dst[d + 13] = -(ux * ex + uy * ey + uz * ez);
    dst[d + 14] = fx * ex + fy * ey + fz * ez;
    dst[d + 15] = 1;
  }
}

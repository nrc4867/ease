package dev.chieppa.ease.math;

/** Allocation-free quaternion operations on packed {@code float[]} data, stored x, y, z, w. */
public final class Quats {
  private static final float NEARLY_PARALLEL = 0.9995f;

  private Quats() {}

  public static void identity(float[] dst, int d) {
    set(dst, d, 0, 0, 0, 1);
  }

  /** Writes the rotation of {@code angle} radians about the axis, which need not be unit. */
  public static void fromAxisAngle(float ax, float ay, float az, float angle, float[] dst, int d) {
    float len = (float) Math.sqrt(ax * ax + ay * ay + az * az);
    if (len == 0) {
      identity(dst, d);
      return;
    }
    float half = angle * 0.5f;
    float s = (float) Trig.sin(half) / len;
    set(dst, d, ax * s, ay * s, az * s, (float) Trig.cos(half));
  }

  /** Writes {@code a * b}: the rotation that applies {@code b} first, then {@code a}. */
  public static void mul(float[] a, int ao, float[] b, int bo, float[] dst, int d) {
    float ax = a[ao];
    float ay = a[ao + 1];
    float az = a[ao + 2];
    float aw = a[ao + 3];
    float bx = b[bo];
    float by = b[bo + 1];
    float bz = b[bo + 2];
    float bw = b[bo + 3];
    float x = aw * bx + ax * bw + ay * bz - az * by;
    float y = aw * by - ax * bz + ay * bw + az * bx;
    float z = aw * bz + ax * by - ay * bx + az * bw;
    float w = aw * bw - ax * bx - ay * by - az * bz;
    set(dst, d, x, y, z, w);
  }

  public static void conjugate(float[] a, int ao, float[] dst, int d) {
    set(dst, d, -a[ao], -a[ao + 1], -a[ao + 2], a[ao + 3]);
  }

  /** Writes the inverse; for a unit quaternion it equals the conjugate. */
  public static void inverse(float[] a, int ao, float[] dst, int d) {
    float inv = 1 / lengthSquared(a, ao);
    set(dst, d, -a[ao] * inv, -a[ao + 1] * inv, -a[ao + 2] * inv, a[ao + 3] * inv);
  }

  public static float lengthSquared(float[] a, int ao) {
    float x = a[ao];
    float y = a[ao + 1];
    float z = a[ao + 2];
    float w = a[ao + 3];
    return x * x + y * y + z * z + w * w;
  }

  /** Writes the quaternion scaled to length 1, or the identity when its length is zero. */
  public static void normalize(float[] a, int ao, float[] dst, int d) {
    writeNormalized(dst, d, a[ao], a[ao + 1], a[ao + 2], a[ao + 3]);
  }

  /** Writes the rotation of the vector at {@code vo} by the unit quaternion at {@code qo}. */
  public static void rotate(float[] q, int qo, float[] v, int vo, float[] dst, int d) {
    float qx = q[qo];
    float qy = q[qo + 1];
    float qz = q[qo + 2];
    float qw = q[qo + 3];
    float vx = v[vo];
    float vy = v[vo + 1];
    float vz = v[vo + 2];
    float tx = 2 * (qy * vz - qz * vy);
    float ty = 2 * (qz * vx - qx * vz);
    float tz = 2 * (qx * vy - qy * vx);
    float x = vx + qw * tx + (qy * tz - qz * ty);
    float y = vy + qw * ty + (qz * tx - qx * tz);
    float z = vz + qw * tz + (qx * ty - qy * tx);
    Vec3s.set(dst, d, x, y, z);
  }

  /** Writes the shortest-path interpolation from {@code a} to {@code b}. */
  public static void slerp(float[] a, int ao, float[] b, int bo, float t, float[] dst, int d) {
    float bx = b[bo];
    float by = b[bo + 1];
    float bz = b[bo + 2];
    float bw = b[bo + 3];
    float dot = a[ao] * bx + a[ao + 1] * by + a[ao + 2] * bz + a[ao + 3] * bw;
    if (dot < 0) {
      bx = -bx;
      by = -by;
      bz = -bz;
      bw = -bw;
      dot = -dot;
    }
    float wa = 1 - t;
    float wb = t;
    if (dot < NEARLY_PARALLEL) {
      float theta = (float) StrictMath.acos(dot);
      float invSin = 1 / (float) Trig.sin(theta);
      wa = (float) Trig.sin((1 - t) * theta) * invSin;
      wb = (float) Trig.sin(t * theta) * invSin;
    }
    float x = wa * a[ao] + wb * bx;
    float y = wa * a[ao + 1] + wb * by;
    float z = wa * a[ao + 2] + wb * bz;
    float w = wa * a[ao + 3] + wb * bw;
    writeNormalized(dst, d, x, y, z, w);
  }

  /** Writes the four components at {@code d}. */
  public static void set(float[] dst, int d, float x, float y, float z, float w) {
    dst[d] = x;
    dst[d + 1] = y;
    dst[d + 2] = z;
    dst[d + 3] = w;
  }

  private static void writeNormalized(float[] dst, int d, float x, float y, float z, float w) {
    float len = (float) Math.sqrt(x * x + y * y + z * z + w * w);
    if (len == 0) {
      identity(dst, d);
      return;
    }
    float inv = 1 / len;
    set(dst, d, x * inv, y * inv, z * inv, w * inv);
  }
}

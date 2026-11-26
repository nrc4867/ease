package dev.chieppa.ease.math;

/** A rotation as a quaternion with components x, y, z, w; rotations are unit quaternions. */
public record Quat(float x, float y, float z, float w) {
  public static final Quat IDENTITY = new Quat(0, 0, 0, 1);

  private static final float NEARLY_PARALLEL = 0.9995f;

  /** The rotation of {@code angle} radians about {@code axis}, which need not be unit. */
  public static Quat fromAxisAngle(Vec3 axis, float angle) {
    float len = axis.length();
    if (len == 0) {
      return IDENTITY;
    }
    float half = angle * 0.5f;
    float s = (float) Trig.sin(half) / len;
    return new Quat(axis.x() * s, axis.y() * s, axis.z() * s, (float) Trig.cos(half));
  }

  /** The rotation that applies {@code o} first, then this one. */
  public Quat mul(Quat o) {
    float nx = w * o.x + x * o.w + y * o.z - z * o.y;
    float ny = w * o.y - x * o.z + y * o.w + z * o.x;
    float nz = w * o.z + x * o.y - y * o.x + z * o.w;
    float nw = w * o.w - x * o.x - y * o.y - z * o.z;
    return new Quat(nx, ny, nz, nw);
  }

  public Quat conjugate() {
    return new Quat(-x, -y, -z, w);
  }

  public Quat inverse() {
    float inv = 1 / (x * x + y * y + z * z + w * w);
    return new Quat(-x * inv, -y * inv, -z * inv, w * inv);
  }

  /** The quaternion scaled to length 1, or the identity when its length is zero. */
  public Quat normalize() {
    float len = (float) Math.sqrt(x * x + y * y + z * z + w * w);
    if (len == 0) {
      return IDENTITY;
    }
    float inv = 1 / len;
    return new Quat(x * inv, y * inv, z * inv, w * inv);
  }

  public Vec3 rotate(Vec3 v) {
    float tx = 2 * (y * v.z() - z * v.y());
    float ty = 2 * (z * v.x() - x * v.z());
    float tz = 2 * (x * v.y() - y * v.x());
    float rx = v.x() + w * tx + (y * tz - z * ty);
    float ry = v.y() + w * ty + (z * tx - x * tz);
    float rz = v.z() + w * tz + (x * ty - y * tx);
    return new Vec3(rx, ry, rz);
  }

  /** The shortest-path interpolation from this rotation to {@code o}. */
  public Quat slerp(Quat o, float t) {
    float dot = x * o.x + y * o.y + z * o.z + w * o.w;
    float sign = dot < 0 ? -1 : 1;
    float cos = Math.abs(dot);
    float wa = 1 - t;
    float wb = t;
    if (cos < NEARLY_PARALLEL) {
      float theta = (float) StrictMath.acos(cos);
      float invSin = 1 / (float) Trig.sin(theta);
      wa = (float) Trig.sin((1 - t) * theta) * invSin;
      wb = (float) Trig.sin(t * theta) * invSin;
    }
    wb *= sign;
    return new Quat(wa * x + wb * o.x, wa * y + wb * o.y, wa * z + wb * o.z, wa * w + wb * o.w)
        .normalize();
  }

  public Mat4 toMat4() {
    return Mat4.rotation(this);
  }

  /** True when every component differs from the other quaternion's by at most the tolerance. */
  public boolean approxEquals(Quat o, float tolerance) {
    return Math.abs(x - o.x) <= tolerance
        && Math.abs(y - o.y) <= tolerance
        && Math.abs(z - o.z) <= tolerance
        && Math.abs(w - o.w) <= tolerance;
  }

  public void writeTo(float[] dst, int offset) {
    Quats.set(dst, offset, x, y, z, w);
  }

  public static Quat readFrom(float[] src, int offset) {
    return new Quat(src[offset], src[offset + 1], src[offset + 2], src[offset + 3]);
  }
}

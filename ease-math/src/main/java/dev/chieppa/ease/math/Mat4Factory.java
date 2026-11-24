package dev.chieppa.ease.math;

/** Builds the {@link Mat4} transforms, projections and views, computed on scalars. */
final class Mat4Factory {
  private Mat4Factory() {}

  static Mat4 fromTransform(Vec3 t, Quat q, Vec3 s) {
    float xx = q.x() * q.x();
    float yy = q.y() * q.y();
    float zz = q.z() * q.z();
    float xy = q.x() * q.y();
    float xz = q.x() * q.z();
    float yz = q.y() * q.z();
    float wx = q.w() * q.x();
    float wy = q.w() * q.y();
    float wz = q.w() * q.z();
    return new Mat4(
        (1 - 2 * (yy + zz)) * s.x(),
        2 * (xy + wz) * s.x(),
        2 * (xz - wy) * s.x(),
        0,
        2 * (xy - wz) * s.y(),
        (1 - 2 * (xx + zz)) * s.y(),
        2 * (yz + wx) * s.y(),
        0,
        2 * (xz + wy) * s.z(),
        2 * (yz - wx) * s.z(),
        (1 - 2 * (xx + yy)) * s.z(),
        0,
        t.x(),
        t.y(),
        t.z(),
        1);
  }

  static Mat4 perspective(float fovY, float aspect, float near, float far) {
    float f = 1 / (float) Math.tan(fovY * 0.5f);
    float range = near - far;
    return new Mat4(
        f / aspect, 0, 0, 0, 0, f, 0, 0, 0, 0, far / range, -1, 0, 0, near * far / range, 0);
  }

  static Mat4 orthographic(
      float left, float right, float bottom, float top, float near, float far) {
    float range = near - far;
    return new Mat4(
        2 / (right - left),
        0,
        0,
        0,
        0,
        2 / (top - bottom),
        0,
        0,
        0,
        0,
        1 / range,
        0,
        -(right + left) / (right - left),
        -(top + bottom) / (top - bottom),
        near / range,
        1);
  }

  static Mat4 lookAt(Vec3 eye, Vec3 target, Vec3 up) {
    Vec3 f = target.sub(eye).normalize();
    Vec3 s = f.cross(up).normalize();
    Vec3 u = s.cross(f);
    return new Mat4(
        s.x(),
        u.x(),
        -f.x(),
        0,
        s.y(),
        u.y(),
        -f.y(),
        0,
        s.z(),
        u.z(),
        -f.z(),
        0,
        -s.dot(eye),
        -u.dot(eye),
        f.dot(eye),
        1);
  }
}

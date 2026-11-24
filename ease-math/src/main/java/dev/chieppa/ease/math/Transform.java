package dev.chieppa.ease.math;

/**
 * Translation, rotation and scale, applied in the order scale, rotate, translate. Composition and
 * inversion are exact when the outer scale is uniform and approximate for a non-uniform scale under
 * a rotation, which a translation-rotation-scale triple cannot represent.
 */
public record Transform(Vec3 translation, Quat rotation, Vec3 scale) {
  public static final Transform IDENTITY = new Transform(Vec3.ZERO, Quat.IDENTITY, Vec3.ONE);

  /** The transform that applies {@code child} first, then this one (this is the parent). */
  public Transform compose(Transform child) {
    Vec3 scaled = componentMul(scale, child.translation);
    Vec3 moved = translation.add(rotation.rotate(scaled));
    return new Transform(moved, rotation.mul(child.rotation), componentMul(scale, child.scale));
  }

  public Transform inverse() {
    Quat inverseRotation = rotation.conjugate();
    Vec3 inverseScale = new Vec3(1 / scale.x(), 1 / scale.y(), 1 / scale.z());
    Vec3 rotated = inverseRotation.rotate(translation.negate());
    return new Transform(componentMul(inverseScale, rotated), inverseRotation, inverseScale);
  }

  public Mat4 toMat4() {
    return Mat4.fromTransform(translation, rotation, scale);
  }

  public Vec3 transformPoint(Vec3 p) {
    return translation.add(rotation.rotate(componentMul(scale, p)));
  }

  /** Lerps translation and scale and slerps rotation from {@code a} to {@code b}. */
  public static Transform interpolate(Transform a, Transform b, float t) {
    return new Transform(
        a.translation.lerp(b.translation, t),
        a.rotation.slerp(b.rotation, t),
        a.scale.lerp(b.scale, t));
  }

  private static Vec3 componentMul(Vec3 a, Vec3 b) {
    return new Vec3(a.x() * b.x(), a.y() * b.y(), a.z() * b.z());
  }
}

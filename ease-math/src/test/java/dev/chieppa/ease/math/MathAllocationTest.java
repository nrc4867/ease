package dev.chieppa.ease.math;

import static dev.chieppa.ease.math.MathAssert.sampleTransform;

import dev.chieppa.ease.testing.AllocationBudget;
import org.junit.jupiter.api.Test;

class MathAllocationTest {
  private final float[] a = new float[32];
  private final float[] b = new float[32];
  private final float[] dst = new float[32];

  MathAllocationTest() {
    Transform t = sampleTransform();
    t.toMat4().writeTo(a, 0);
    t.toMat4().writeTo(a, 16);
    Mat4.perspective(1f, 1.5f, 0.1f, 50f).writeTo(b, 0);
    Mat4.perspective(1f, 1.5f, 0.1f, 50f).writeTo(b, 16);
    float[] unit = {0.5f, 0.5f, 0.5f, 0.5f};
    System.arraycopy(unit, 0, a, 0, 4);
    System.arraycopy(unit, 0, b, 0, 4);
  }

  @Test
  void vec3Operations() {
    AllocationBudget.assertNoAllocation(() -> Vec3s.add(a, 0, b, 0, dst, 0));
    AllocationBudget.assertNoAllocation(() -> Vec3s.sub(a, 0, b, 0, dst, 0));
    AllocationBudget.assertNoAllocation(() -> Vec3s.scale(a, 0, 2f, dst, 0));
    AllocationBudget.assertNoAllocation(() -> Vec3s.negate(a, 0, dst, 0));
    AllocationBudget.assertNoAllocation(() -> Vec3s.dot(a, 0, b, 0));
    AllocationBudget.assertNoAllocation(() -> Vec3s.cross(a, 0, b, 0, dst, 0));
    AllocationBudget.assertNoAllocation(() -> Vec3s.lengthSquared(a, 0));
    AllocationBudget.assertNoAllocation(() -> Vec3s.length(a, 0));
    AllocationBudget.assertNoAllocation(() -> Vec3s.distance(a, 0, b, 0));
    AllocationBudget.assertNoAllocation(() -> Vec3s.normalize(a, 0, dst, 0));
    AllocationBudget.assertNoAllocation(() -> Vec3s.lerp(a, 0, b, 0, 0.3f, dst, 0));
    AllocationBudget.assertNoAllocation(() -> Vec3s.min(a, 0, b, 0, dst, 0));
    AllocationBudget.assertNoAllocation(() -> Vec3s.max(a, 0, b, 0, dst, 0));
  }

  @Test
  void quatOperations() {
    AllocationBudget.assertNoAllocation(() -> Quats.identity(dst, 0));
    AllocationBudget.assertNoAllocation(() -> Quats.fromAxisAngle(0, 1, 0, 0.5f, dst, 0));
    AllocationBudget.assertNoAllocation(() -> Quats.mul(a, 0, b, 0, dst, 0));
    AllocationBudget.assertNoAllocation(() -> Quats.conjugate(a, 0, dst, 0));
    AllocationBudget.assertNoAllocation(() -> Quats.inverse(a, 0, dst, 0));
    AllocationBudget.assertNoAllocation(() -> Quats.normalize(a, 0, dst, 0));
    AllocationBudget.assertNoAllocation(() -> Quats.rotate(a, 0, b, 4, dst, 0));
    AllocationBudget.assertNoAllocation(() -> Quats.slerp(a, 0, b, 0, 0.4f, dst, 0));
  }

  @Test
  void mat4Construction() {
    AllocationBudget.assertNoAllocation(() -> Mat4s.identity(dst, 0));
    AllocationBudget.assertNoAllocation(() -> Mat4s.translation(1, 2, 3, dst, 0));
    AllocationBudget.assertNoAllocation(() -> Mat4s.scaling(1, 2, 3, dst, 0));
    AllocationBudget.assertNoAllocation(() -> Mat4s.rotation(a, 0, dst, 0));
    AllocationBudget.assertNoAllocation(() -> Mat4s.fromTransform(a, 4, a, 0, a, 8, dst, 0));
    AllocationBudget.assertNoAllocation(() -> Mat4Cameras.perspective(1f, 1.5f, 0.1f, 9f, dst, 0));
    AllocationBudget.assertNoAllocation(
        () -> Mat4Cameras.orthographic(-1, 1, -1, 1, 0.1f, 9f, dst, 0));
    AllocationBudget.assertNoAllocation(() -> Mat4Cameras.lookAt(a, 4, a, 8, b, 12, dst, 0));
  }

  @Test
  void mat4Operations() {
    AllocationBudget.assertNoAllocation(() -> Mat4s.mul(a, 0, b, 16, dst, 0));
    AllocationBudget.assertNoAllocation(() -> Mat4s.transpose(a, 0, dst, 0));
    AllocationBudget.assertNoAllocation(() -> Mat4s.transformPoint(a, 0, b, 0, dst, 0));
    AllocationBudget.assertNoAllocation(() -> Mat4s.transformDirection(a, 0, b, 0, dst, 0));
    AllocationBudget.assertNoAllocation(() -> Mat4s.transformVec4(a, 0, b, 0, dst, 0));
    AllocationBudget.assertNoAllocation(() -> Mat4Inverse.determinant(a, 16));
    AllocationBudget.assertNoAllocation(() -> Mat4Inverse.inverse(a, 16, dst, 0));
    AllocationBudget.assertNoAllocation(() -> Mat4Inverse.inverseAffine(a, 16, dst, 0));
  }

  @Test
  void aabbOperations() {
    AllocationBudget.assertNoAllocation(() -> Aabbs.ofPoints(a, 0, 4, dst, 0));
    AllocationBudget.assertNoAllocation(() -> Aabbs.union(a, 0, b, 0, dst, 0));
    AllocationBudget.assertNoAllocation(() -> Aabbs.containsPoint(a, 0, b, 6));
    AllocationBudget.assertNoAllocation(() -> Aabbs.intersects(a, 0, b, 0));
    AllocationBudget.assertNoAllocation(() -> Aabbs.transform(a, 0, a, 16, dst, 0));
  }
}

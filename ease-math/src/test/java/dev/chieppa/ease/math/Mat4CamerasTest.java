package dev.chieppa.ease.math;

import static dev.chieppa.ease.math.MathAssert.EPS;
import static dev.chieppa.ease.math.MathAssert.assertVec3;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class Mat4CamerasTest {
  private static Vec3 project(Mat4 m, Vec3 p) {
    Vec4 clip = m.transform(new Vec4(p.x(), p.y(), p.z(), 1));
    return new Vec3(clip.x() / clip.w(), clip.y() / clip.w(), clip.z() / clip.w());
  }

  @Test
  void perspectiveMapsNearToZeroAndFarToOne() {
    Mat4 m = Mat4.perspective((float) Math.PI / 2, 2f, 0.5f, 100f);
    assertEquals(0f, project(m, new Vec3(0, 0, -0.5f)).z(), EPS);
    assertEquals(1f, project(m, new Vec3(0, 0, -100f)).z(), EPS);
  }

  @Test
  void perspectiveFieldOfViewReachesTheClipEdges() {
    Mat4 m = Mat4.perspective((float) Math.PI / 2, 2f, 0.5f, 100f);
    assertEquals(1f, project(m, new Vec3(0, 10, -10)).y(), EPS);
    assertEquals(1f, project(m, new Vec3(20, 0, -10)).x(), EPS);
  }

  @Test
  void orthographicMapsTheBoxToTheClipVolume() {
    Mat4 m = Mat4.orthographic(-2, 2, -1, 1, 1, 11);
    assertVec3(-1, -1, 0, project(m, new Vec3(-2, -1, -1)));
    assertVec3(1, 1, 1, project(m, new Vec3(2, 1, -11)));
    assertVec3(0, 0, 0.5f, project(m, new Vec3(0, 0, -6)));
  }

  @Test
  void lookAtPutsTheEyeAtTheOriginFacingMinusZ() {
    Vec3 eye = new Vec3(3, 4, 5);
    Mat4 view = Mat4.lookAt(eye, new Vec3(3, 4, -5), Vec3.UNIT_Y);
    assertVec3(0, 0, 0, view.transformPoint(eye));
    assertVec3(0, 0, -10, view.transformPoint(new Vec3(3, 4, -5)));
  }

  @Test
  void lookAtKeepsUpPointingUpAndRightToTheRight() {
    Mat4 view = Mat4.lookAt(Vec3.ZERO, new Vec3(-1, 0, 0), Vec3.UNIT_Y);
    assertVec3(0, 1, 0, view.transformDirection(Vec3.UNIT_Y));
    assertVec3(0, 0, -1, view.transformDirection(new Vec3(-1, 0, 0)));
    assertVec3(1, 0, 0, view.transformDirection(new Vec3(0, 0, -1)));
  }
}

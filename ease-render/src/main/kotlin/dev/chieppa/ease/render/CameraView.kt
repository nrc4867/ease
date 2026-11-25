package dev.chieppa.ease.render

import dev.chieppa.ease.gpu.lit.MeshFrame
import dev.chieppa.ease.math.Mat4Cameras
import dev.chieppa.ease.math.Mat4s
import dev.chieppa.ease.platform.PixelSize

private const val MATRIX = 16

/** The w component's place in a quaternion. */
private const val W = 3

/**
 * Turns a camera's transform and lens into a frame's view-projection matrix and eye position, in
 * scratch arrays made once.
 */
internal class CameraView {
  private val conjugate = FloatArray(4)
  private val rotation = FloatArray(MATRIX)
  private val translation = FloatArray(MATRIX)
  private val view = FloatArray(MATRIX)
  private val projection = FloatArray(MATRIX)

  /**
   * Writes into [frame] the view of a camera at [transform] ([TRANSFORM_FLOATS] floats, scale
   * ignored) with [camera]'s lens, for a target of [size].
   */
  fun write(transform: FloatArray, camera: SnapshotCamera, size: PixelSize, frame: MeshFrame) {
    val x = transform[0]
    val y = transform[1]
    val z = transform[2]
    conjugate[0] = -transform[ROTATION_AT]
    conjugate[1] = -transform[ROTATION_AT + 1]
    conjugate[2] = -transform[ROTATION_AT + 2]
    conjugate[W] = transform[ROTATION_AT + W]
    Mat4s.rotation(conjugate, 0, rotation, 0)
    Mat4s.translation(-x, -y, -z, translation, 0)
    Mat4s.mul(rotation, 0, translation, 0, view, 0)
    val aspect = size.width.toFloat() / size.height
    val fieldOfView = Math.toRadians(camera.fieldOfView.toDouble()).toFloat()
    Mat4Cameras.perspective(fieldOfView, aspect, camera.near, camera.far, projection, 0)
    Mat4s.mul(projection, 0, view, 0, frame.viewProjection, 0)
    frame.cameraPosition[0] = x
    frame.cameraPosition[1] = y
    frame.cameraPosition[2] = z
  }
}

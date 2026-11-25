package dev.chieppa.ease.sample

import dev.chieppa.ease.math.Mat4
import dev.chieppa.ease.math.Quat
import dev.chieppa.ease.math.Vec3
import dev.chieppa.ease.math.Vec4
import dev.chieppa.ease.scene.document.SceneNode

/**
 * Where world points land in a [width] by [height] image seen through the scene's [camera] node, as
 * its node file holds it: the same view and perspective the renderer builds.
 */
internal class CameraProjection(
  camera: SceneNode,
  private val width: Int,
  private val height: Int,
) {
  private val viewProjection: Mat4

  init {
    fun field(component: String, name: String) = SampleContent.field(camera, component, name)
    fun transform(name: String) = field(TRANSFORM, name)
    val eye = Vec3(transform("x"), transform("y"), transform("z"))
    val rotation =
      Quat(
        transform("rotationX"),
        transform("rotationY"),
        transform("rotationZ"),
        transform("rotationW"),
      )
    val view = Mat4.fromTransform(eye, rotation, Vec3.ONE).inverse()
    val fieldOfView = Math.toRadians(field(LENS, "fieldOfView").toDouble()).toFloat()
    val aspect = width.toFloat() / height
    val projection = Mat4.perspective(fieldOfView, aspect, field(LENS, "near"), field(LENS, "far"))
    viewProjection = projection.mul(view)
  }

  /** The column and row world point ([x], [y], [z]) lands on. */
  fun pixel(x: Float, y: Float, z: Float): Pair<Int, Int> {
    val clip = viewProjection.transform(Vec4(x, y, z, 1f))
    val column = (clip.x() / clip.w() + 1f) / 2f * width
    val row = (1f - clip.y() / clip.w()) / 2f * height
    return column.toInt() to row.toInt()
  }

  private companion object {
    const val TRANSFORM = "ease.LocalTransform"
    const val LENS = "ease.Camera"
  }
}

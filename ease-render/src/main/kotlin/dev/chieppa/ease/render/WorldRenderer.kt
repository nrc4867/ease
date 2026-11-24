package dev.chieppa.ease.render

import dev.chieppa.ease.gpu.Color
import dev.chieppa.ease.gpu.CommandEncoder
import dev.chieppa.ease.gpu.GpuContext
import dev.chieppa.ease.gpu.InstancedQuadPass
import dev.chieppa.ease.gpu.QuadInstances
import dev.chieppa.ease.gpu.RenderTarget
import dev.chieppa.ease.gpu.View2D

/** Quads a renderer draws at most unless told otherwise. */
public const val DEFAULT_QUAD_CAPACITY: Int = 4096

/**
 * Draws a [RenderSnapshot] through a camera: the target cleared to [clear], then one quad per
 * entity the snapshot holds, placed between its previous and current tick. Holds room for
 * [capacity] quads, made once, so drawing a frame allocates no Kotlin objects.
 */
public class WorldRenderer(
  context: GpuContext,
  public val capacity: Int = DEFAULT_QUAD_CAPACITY,
  clear: Color = InstancedQuadPass.DEFAULT_CLEAR,
) : AutoCloseable {
  private val pass = InstancedQuadPass(context, capacity, clear)
  private val quads = QuadInstances(capacity)
  private val view = View2D()

  /**
   * Records into [encoder] a frame of [snapshot] seen by [camera] in [target], each quad [alpha] of
   * the way from its previous tick's position to its current one. Fails when the snapshot holds
   * more than [capacity] quads.
   */
  public fun render(
    snapshot: RenderSnapshot,
    alpha: Float,
    camera: Camera2D,
    target: RenderTarget,
    encoder: CommandEncoder,
  ) {
    check(snapshot.count <= capacity) {
      "${snapshot.count} quads do not fit a renderer for $capacity"
    }
    quads.clear()
    for (index in 0 until snapshot.count) {
      val id = snapshot.entityAt(index)
      val quad = quads.add(snapshot.x(id, alpha), snapshot.y(id, alpha), snapshot.size[id])
      quads.setColor(quad, snapshot.red[id], snapshot.green[id], snapshot.blue[id])
    }
    camera.applyTo(view, target.size)
    pass.record(encoder, target, view, quads)
  }

  override fun close() {
    pass.close()
  }
}

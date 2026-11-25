package dev.chieppa.ease.render

import dev.chieppa.ease.gpu.lit.MeshFrame
import dev.chieppa.ease.math.Quats

private const val CHANNELS = 3

/** A light shines down its transform's -z. */
private val FORWARD = floatArrayOf(0f, 0f, -1f)

/**
 * Sets a frame's sun and ambient light from a snapshot's light. Without a light, nothing is lit
 * directly and every surface shows its full base color.
 */
internal object LightSetup {
  /** Writes [snapshot]'s light at [alpha] into [frame], using [scratch] for its transform. */
  fun write(snapshot: RenderSnapshot, alpha: Float, scratch: FloatArray, frame: MeshFrame) {
    val light = snapshot.light
    if (!light.present) {
      frame.lightIntensity = 0f
      frame.ambient.fill(1f)
      return
    }
    snapshot.transformInto(light.entity, alpha, scratch, 0)
    Quats.rotate(scratch, ROTATION_AT, FORWARD, 0, frame.lightDirection, 0)
    for (channel in 0 until CHANNELS) {
      frame.lightColor[channel] = light.color[channel]
      frame.ambient[channel] = light.color[channel] * light.ambient
    }
    frame.lightIntensity = light.intensity
  }
}

package dev.chieppa.ease.animation.graph

import dev.chieppa.ease.animation.assets.ClipAsset
import dev.chieppa.ease.assets.Guid

/** A clip as a graph plays it: the joint each channel drives, and its events by normalized time. */
internal class GraphClip(val guid: Guid, val asset: ClipAsset, val joints: IntArray) {
  /** Each event's time as a share of the clip's duration, 0 to 1. */
  val eventPhases: FloatArray =
    FloatArray(asset.events.size) {
      if (asset.duration > 0f) asset.events[it].time / asset.duration else 0f
    }
}

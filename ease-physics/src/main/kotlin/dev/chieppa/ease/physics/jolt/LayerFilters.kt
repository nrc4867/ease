package dev.chieppa.ease.physics.jolt

import com.github.stephengold.joltjni.BodyFilter
import com.github.stephengold.joltjni.BroadPhaseLayerFilter
import com.github.stephengold.joltjni.ObjectLayerFilter
import com.github.stephengold.joltjni.PhysicsSystem
import com.github.stephengold.joltjni.ShapeFilter
import com.github.stephengold.joltjni.SpecifiedBroadPhaseLayerFilter
import com.github.stephengold.joltjni.SpecifiedObjectLayerFilter

/**
 * The query filters a world reuses, made once: for each collision layer, what that layer collides
 * with; for every solid layer at once; and for trigger volumes alone.
 */
internal class LayerFilters(system: PhysicsSystem, layers: JoltLayers) : AutoCloseable {
  private val solidLayers = layers.trigger

  /** Per collision layer: the broad-phase layers it collides with. */
  val broadPhaseOf: Array<BroadPhaseLayerFilter> =
    Array(solidLayers) { system.getDefaultBroadPhaseLayerFilter(it) }

  /** Per collision layer: the object layers it collides with. */
  val objectsOf: Array<ObjectLayerFilter> = Array(solidLayers) { system.getDefaultLayerFilter(it) }

  /** Every solid layer, and no triggers. */
  val solidBroadPhase: BroadPhaseLayerFilter = SpecifiedBroadPhaseLayerFilter(JoltLayers.SOLID)
  val anyObject: ObjectLayerFilter = ObjectLayerFilter()

  /** Trigger volumes alone. */
  val triggerBroadPhase: BroadPhaseLayerFilter = SpecifiedBroadPhaseLayerFilter(JoltLayers.TRIGGERS)
  val triggerObjects: ObjectLayerFilter = SpecifiedObjectLayerFilter(layers.trigger)

  val anyBody: BodyFilter = BodyFilter()
  val anyShape: ShapeFilter = ShapeFilter()

  override fun close() {
    broadPhaseOf.forEach { it.close() }
    objectsOf.forEach { it.close() }
    listOf(solidBroadPhase, anyObject, triggerBroadPhase, triggerObjects, anyBody, anyShape)
      .forEach { it.close() }
  }
}

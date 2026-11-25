package dev.chieppa.ease.physics.jolt

import com.github.stephengold.joltjni.BroadPhaseLayerInterfaceTable
import com.github.stephengold.joltjni.ObjectLayerPairFilterTable
import com.github.stephengold.joltjni.ObjectVsBroadPhaseLayerFilterTable
import dev.chieppa.ease.physics.setup.CollisionLayers

/**
 * Jolt's layer tables for a game's [layers]: one object layer per collision layer, then one more
 * for trigger volumes, which collides with nothing. Solid layers share one broad-phase layer and
 * triggers have their own, so queries for solids never visit triggers.
 */
internal class JoltLayers(layers: CollisionLayers) : AutoCloseable {
  /** The object layer trigger volumes are on. */
  val trigger: Int = layers.size

  /** How many object layers there are, the trigger layer included. */
  val objectLayers: Int = layers.size + 1

  val pairs: ObjectLayerPairFilterTable = ObjectLayerPairFilterTable(objectLayers)
  val broadPhase: BroadPhaseLayerInterfaceTable =
    BroadPhaseLayerInterfaceTable(objectLayers, BROAD_PHASE_LAYERS)
  val objectVsBroadPhase: ObjectVsBroadPhaseLayerFilterTable

  init {
    for (a in 0 until layers.size) {
      broadPhase.mapObjectToBroadPhaseLayer(a, SOLID)
      for (b in a until layers.size) if (layers.collide(a, b)) pairs.enableCollision(a, b)
    }
    broadPhase.mapObjectToBroadPhaseLayer(trigger, TRIGGERS)
    objectVsBroadPhase =
      ObjectVsBroadPhaseLayerFilterTable(broadPhase, BROAD_PHASE_LAYERS, pairs, objectLayers)
  }

  override fun close() {
    objectVsBroadPhase.close()
    broadPhase.close()
    pairs.close()
  }

  companion object {
    /** The broad-phase layer of every solid object layer. */
    const val SOLID: Int = 0

    /** The broad-phase layer of trigger volumes. */
    const val TRIGGERS: Int = 1

    private const val BROAD_PHASE_LAYERS = 2
  }
}

package dev.chieppa.ease.physics.body

import dev.chieppa.ease.annotations.Doc
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Doc("How a rigid body moves.")
@Serializable
@SerialName("ease.MotionKind")
public enum class MotionKind {
  @Doc("Moved by its node's transform, pushing what it meets; nothing pushes it.") KINEMATIC,
  @Doc("Moved by the simulation: gravity, contacts and impulses; it writes its node's transform.")
  DYNAMIC,
}

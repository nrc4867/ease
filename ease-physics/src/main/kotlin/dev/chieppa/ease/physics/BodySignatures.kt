package dev.chieppa.ease.physics

import dev.chieppa.ease.physics.body.Collider
import dev.chieppa.ease.physics.body.RigidBody
import dev.chieppa.ease.physics.character.CharacterController

/**
 * One number for every field a Jolt body or character is built from, so a system notices, each tick
 * and without allocating, that one changed and builds it again. Equal fields give equal numbers; a
 * change gives another number but for a hash collision.
 */
internal object BodySignatures {
  private const val PRIME = 31

  /** The signature of a collider's body: its fields, its rigid body's motion, whether a trigger. */
  fun collider(collider: Collider, rigid: RigidBody?, trigger: Boolean): Int {
    var hash = collider.shape.ordinal
    hash = PRIME * hash + collider.halfX.toRawBits()
    hash = PRIME * hash + collider.halfY.toRawBits()
    hash = PRIME * hash + collider.halfZ.toRawBits()
    hash = PRIME * hash + collider.radius.toRawBits()
    hash = PRIME * hash + collider.halfHeight.toRawBits()
    hash = PRIME * hash + collider.layer.hashCode()
    hash = PRIME * hash + collider.surface.hashCode()
    hash = PRIME * hash + (rigid?.motion?.ordinal ?: -1)
    return PRIME * hash + if (trigger) 1 else 0
  }

  /** The signature of a character: its controller's fields that Jolt's character is built from. */
  fun controller(controller: CharacterController): Int {
    var hash = controller.radius.toRawBits()
    hash = PRIME * hash + controller.height.toRawBits()
    hash = PRIME * hash + controller.maxSlopeDegrees.toRawBits()
    hash = PRIME * hash + controller.stepHeight.toRawBits()
    return PRIME * hash + controller.layer.hashCode()
  }
}

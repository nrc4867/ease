package dev.chieppa.ease.physics.body

import dev.chieppa.ease.annotations.Doc
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Doc("The shape of a collider, centered on its node.")
@Serializable
@SerialName("ease.ShapeKind")
public enum class ShapeKind {
  @Doc("A box with the collider's half extents.") BOX,
  @Doc("A sphere with the collider's radius.") SPHERE,
  @Doc("An upright capsule: the collider's radius around a segment of twice its half height.")
  CAPSULE,
}

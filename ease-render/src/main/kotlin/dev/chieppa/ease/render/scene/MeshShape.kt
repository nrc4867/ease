package dev.chieppa.ease.render.scene

import dev.chieppa.ease.annotations.Doc
import kotlinx.serialization.Serializable

@Doc("A built-in mesh: each fits a unit box centered on its node.")
@Serializable
public enum class MeshShape {
  @Doc("A cube with sides 1 long.") CUBE,
  @Doc("A square 1 on a side in the x-z plane, facing up +y.") PLANE,
  @Doc("A sphere 1 across.") SPHERE,
}

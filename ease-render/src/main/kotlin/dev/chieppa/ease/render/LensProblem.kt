package dev.chieppa.ease.render

import dev.chieppa.ease.render.scene.Camera

/**
 * Why a [Camera]'s lens cannot be drawn through: a field of view outside [Camera.MIN_FIELD_OF_VIEW]
 * to [Camera.MAX_FIELD_OF_VIEW] degrees, a near distance not above 0, or a far distance not beyond
 * near. A renderer whose camera has one draws nothing but the clear color and reports [message].
 */
public enum class LensProblem(public val message: String) {
  FIELD_OF_VIEW(
    "The camera's field of view is outside ${Camera.MIN_FIELD_OF_VIEW} to " +
      "${Camera.MAX_FIELD_OF_VIEW} degrees, so nothing is drawn through it"
  ),
  NEAR("The camera's near distance is not above 0, so nothing is drawn through it"),
  FAR("The camera's far distance is not beyond its near distance, so nothing is drawn through it");

  public companion object {
    /** The problem with a lens of [fieldOfView] degrees, [near] and [far], or null for none. */
    public fun of(fieldOfView: Float, near: Float, far: Float): LensProblem? =
      when {
        !(fieldOfView >= Camera.MIN_FIELD_OF_VIEW && fieldOfView <= Camera.MAX_FIELD_OF_VIEW) ->
          FIELD_OF_VIEW
        !(near > 0f) -> NEAR
        !(far > near) -> FAR
        else -> null
      }
  }
}

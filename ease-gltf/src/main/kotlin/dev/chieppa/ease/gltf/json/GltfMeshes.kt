package dev.chieppa.ease.gltf.json

import dev.chieppa.ease.annotations.Doc
import kotlinx.serialization.Serializable

/** A mesh: one or more primitives, each drawn with its own material. */
@Doc("A mesh: one or more primitives, each drawn with its own material.")
@Serializable
public class MeshJson(
  @Doc("The glTF property primitives.") public val primitives: List<PrimitiveJson>,
  @Doc("The glTF property name.") public val name: String? = null,
  @Doc("The glTF property weights.") public val weights: List<Float>? = null,
)

/**
 * One draw of a mesh: its vertex [attributes] by semantic (`POSITION`, `NORMAL`, `TEXCOORD_0`) as
 * accessor indices, its optional [indices] accessor, [material] and [mode] (4 is triangles).
 */
@Doc(
  "One draw of a mesh: its vertex attributes by semantic (POSITION, NORMAL, TEXCOORD_0) as " +
    "accessor indices, its optional indices accessor, material and mode (4 is triangles)."
)
@Serializable
public class PrimitiveJson(
  @Doc("The glTF property attributes.") public val attributes: Map<String, Int>,
  @Doc("The glTF property indices.") public val indices: Int? = null,
  @Doc("The glTF property material.") public val material: Int? = null,
  @Doc("The glTF property mode.") public val mode: Int = TRIANGLES,
  @Doc("The glTF property targets.") public val targets: List<Map<String, Int>>? = null,
) {
  public companion object {
    /** The mode of a triangle list, the default. */
    public const val TRIANGLES: Int = 4
  }
}

/** A skin: the [joints] it binds and its inverse bind matrices accessor. */
@Doc("A skin: the joints it binds and its inverse bind matrices accessor.")
@Serializable
public class SkinJson(
  @Doc("The glTF property joints.") public val joints: List<Int>,
  @Doc("The glTF property inverseBindMatrices.") public val inverseBindMatrices: Int? = null,
  @Doc("The glTF property skeleton.") public val skeleton: Int? = null,
  @Doc("The glTF property name.") public val name: String? = null,
)

/** A perspective or orthographic camera. */
@Doc("A perspective or orthographic camera.")
@Serializable
public class CameraJson(
  @Doc("The glTF property type.") public val type: String,
  @Doc("The glTF property perspective.") public val perspective: PerspectiveJson? = null,
  @Doc("The glTF property orthographic.") public val orthographic: OrthographicJson? = null,
  @Doc("The glTF property name.") public val name: String? = null,
)

/**
 * A perspective lens: vertical field of view in radians and clip distances; no zfar is infinite.
 */
@Doc(
  "A perspective lens: vertical field of view in radians and clip distances; no zfar is " +
    "infinite."
)
@Serializable
public class PerspectiveJson(
  @Doc("The glTF property yfov.") public val yfov: Float,
  @Doc("The glTF property znear.") public val znear: Float,
  @Doc("The glTF property zfar.") public val zfar: Float? = null,
  @Doc("The glTF property aspectRatio.") public val aspectRatio: Float? = null,
)

/** An orthographic lens: half extents and clip distances. */
@Doc("An orthographic lens: half extents and clip distances.")
@Serializable
public class OrthographicJson(
  @Doc("The glTF property xmag.") public val xmag: Float,
  @Doc("The glTF property ymag.") public val ymag: Float,
  @Doc("The glTF property znear.") public val znear: Float,
  @Doc("The glTF property zfar.") public val zfar: Float,
)

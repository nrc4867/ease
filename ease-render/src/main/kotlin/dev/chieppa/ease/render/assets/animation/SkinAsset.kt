package dev.chieppa.ease.render.assets.animation

/**
 * A cooked skin: the joints a skinned mesh's joint indices name, each by the local id (16 hex
 * digits) of its node in the model's prefab, with its name and its inverse bind matrix (16 floats,
 * column-major, packed joint after joint in [inverseBindMatrices]). [skeleton] is the local id of
 * the skeleton's root node, or null when the file names none.
 */
public class SkinAsset(
  public val joints: List<String>,
  public val jointNames: List<String>,
  public val inverseBindMatrices: FloatArray,
  public val skeleton: String?,
)

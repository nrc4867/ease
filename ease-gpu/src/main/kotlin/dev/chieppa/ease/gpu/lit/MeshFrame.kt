package dev.chieppa.ease.gpu.lit

import dev.chieppa.ease.gpu.Color
import dev.chieppa.ease.gpu.mesh.ComputedMesh
import dev.chieppa.ease.gpu.mesh.MeshData
import dev.chieppa.ease.gpu.mesh.SkinVertices
import dev.chieppa.ease.memory.MAT4_FLOATS

/**
 * What a [LitMeshPass] draws in one frame, filled again by a renderer each frame without
 * allocating: the camera, the sun and ambient light, the clear color, and up to [capacity] draws,
 * each a mesh with its transforms and material, and the joint matrices of up to [jointCapacity]
 * joints for the draws that are skinned. Matrices are column-major float arrays, so a renderer
 * computes them in place with ease-math's array forms.
 */
public class MeshFrame(
  public val capacity: Int,
  public val jointCapacity: Int = DEFAULT_JOINT_CAPACITY,
) {
  init {
    require(capacity > 0) { "Needs room for at least 1 draw, got $capacity" }
    require(jointCapacity > 0) { "Needs room for at least 1 joint, got $jointCapacity" }
  }

  /** The camera's view-projection matrix. */
  public val viewProjection: FloatArray = FloatArray(MAT4_FLOATS)

  /** The camera's world position, x, y and z. */
  public val cameraPosition: FloatArray = FloatArray(3)

  /** The unit direction the sun's light travels, x, y and z. */
  public val lightDirection: FloatArray = floatArrayOf(0f, -1f, 0f)

  /** The sun's linear color, red, green and blue. */
  public val lightColor: FloatArray = floatArrayOf(1f, 1f, 1f)

  /** How strong the sun is. */
  public var lightIntensity: Float = 0f

  /** Linear light added to every surface whatever its direction, red, green and blue. */
  public val ambient: FloatArray = FloatArray(3)

  /** The color the target is cleared to. */
  public var clear: Color = DEFAULT_CLEAR

  /** Each draw's model matrix, 16 floats a draw. */
  public val models: FloatArray = FloatArray(capacity * MAT4_FLOATS)

  /** Each draw's normal matrix, 16 floats a draw. */
  public val normals: FloatArray = FloatArray(capacity * MAT4_FLOATS)

  /** Each draw's material, packed as [MaterialFloats] says. */
  public val materials: FloatArray = FloatArray(capacity * MaterialFloats.COUNT)
  /** The skinned draws' joint matrices, 16 floats a joint, [jointCount] of them in use. */
  public val joints: FloatArray = FloatArray(jointCapacity * MAT4_FLOATS)

  /** How many joint matrices the frame's skinned draws use. */
  public var jointCount: Int = 0
    private set

  internal val meshes = arrayOfNulls<MeshData>(capacity)
  internal val skins = arrayOfNulls<SkinVertices>(capacity)
  internal val jointOffsets = IntArray(capacity)
  internal val computed = arrayOfNulls<ComputedMesh>(capacity)
  private val textureSets = arrayOfNulls<MaterialTextures>(capacity)

  /** How many draws the frame holds. */
  public var count: Int = 0
    private set

  /** Removes every draw. */
  public fun clearDraws() {
    meshes.fill(null, 0, count)
    computed.fill(null, 0, count)
    textureSets.fill(null, 0, count)
    skins.fill(null, 0, count)
    jointOffsets.fill(0, 0, count)
    jointCount = 0
    count = 0
  }

  /**
   * Adds a draw of [mesh] and returns its index; its matrices start at index times 16 in [models]
   * and [normals], and its material is set with [material]. Fails when the frame is full.
   */
  public fun add(mesh: MeshData): Int {
    check(count < capacity) { "More than $capacity draws; make the frame larger" }
    meshes[count] = mesh
    return count++
  }

  /**
   * Adds a draw of [mesh], which must live on the device of the pass drawing this frame, and
   * returns its index, as [add] does for a [MeshData].
   */
  public fun add(mesh: ComputedMesh): Int {
    require(!mesh.closed) { "A closed computed mesh cannot be drawn" }
    check(count < capacity) { "More than $capacity draws; make the frame larger" }
    computed[count] = mesh
    return count++
  }

  /** The mesh of draw [index], or null when the draw is of a [ComputedMesh]. */
  public fun mesh(index: Int): MeshData? {
    require(index in 0 until count) { "No draw $index among $count" }
    return meshes[index]
  }

  /**
   * Skins draw [index], a [MeshData] draw: its vertices follow [skin]'s joints, whose [joints]
   * matrices (16 floats each, in the draw's own space) are copied from [palette] at [from]. Returns
   * false, leaving the draw unskinned, when the frame's joints are full or [skin] does not have the
   * mesh's vertex count.
   */
  public fun skin(
    index: Int,
    skin: SkinVertices,
    palette: FloatArray,
    from: Int,
    joints: Int,
  ): Boolean {
    require(index in 0 until count) { "No draw $index among $count" }
    val fits =
      jointCount + joints <= jointCapacity && skin.vertexCount == meshes[index]?.vertexCount
    if (fits) {
      System.arraycopy(palette, from, this.joints, jointCount * MAT4_FLOATS, joints * MAT4_FLOATS)
      skins[index] = skin
      jointOffsets[index] = jointCount
      jointCount += joints
    }
    return fits
  }

  /** The skin draw [index] is bent by, or null when it is unskinned. */
  public fun skin(index: Int): SkinVertices? = skins[index]

  /** Sets the textures draw [index] samples; null samples the pass's defaults. */
  public fun textures(index: Int, textures: MaterialTextures?) {
    require(index in 0 until count) { "No draw $index among $count" }
    textureSets[index] = textures
  }

  /** The textures draw [index] samples, or null for the pass's defaults. */
  public fun textures(index: Int): MaterialTextures? = textureSets[index]

  /**
   * Sets draw [index]'s lit material: linear base color, roughness and metallic from 0 to 1, and
   * emitted linear light.
   */
  @Suppress("LongParameterList") // A material's eight floats, set without an object.
  public fun material(
    index: Int,
    red: Float,
    green: Float,
    blue: Float,
    roughness: Float,
    metallic: Float,
    emissiveRed: Float,
    emissiveGreen: Float,
    emissiveBlue: Float,
  ) {
    require(index in 0 until count) { "No draw $index among $count" }
    val at = index * MaterialFloats.COUNT
    materials[at] = red
    materials[at + 1] = green
    materials[at + 2] = blue
    materials[at + MaterialFloats.ROUGHNESS] = roughness
    materials[at + MaterialFloats.METALLIC] = metallic
    materials[at + MaterialFloats.EMISSIVE] = emissiveRed
    materials[at + MaterialFloats.EMISSIVE + 1] = emissiveGreen
    materials[at + MaterialFloats.EMISSIVE + 2] = emissiveBlue
  }

  public companion object {
    /** The color a frame clears to unless told otherwise: a dark slate. */
    public val DEFAULT_CLEAR: Color = Color(0.08f, 0.09f, 0.12f)

    /** Joint matrices a frame holds unless told otherwise. */
    public const val DEFAULT_JOINT_CAPACITY: Int = 4096
  }
}

package dev.chieppa.ease.gpu.lit

import dev.chieppa.ease.gpu.Color
import dev.chieppa.ease.gpu.mesh.MeshData

/** Floats a 4x4 matrix takes. */
internal const val MATRIX_FLOATS = 16

/**
 * What a [LitMeshPass] draws in one frame, filled again by a renderer each frame without
 * allocating: the camera, the sun and ambient light, the clear color, and up to [capacity] draws,
 * each a mesh with its transforms and material. Matrices are column-major float arrays, so a
 * renderer computes them in place with ease-math's array forms.
 */
public class MeshFrame(public val capacity: Int) {
  init {
    require(capacity > 0) { "Needs room for at least 1 draw, got $capacity" }
  }

  /** The camera's view-projection matrix. */
  public val viewProjection: FloatArray = FloatArray(MATRIX_FLOATS)

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
  public val models: FloatArray = FloatArray(capacity * MATRIX_FLOATS)

  /** Each draw's normal matrix, 16 floats a draw. */
  public val normals: FloatArray = FloatArray(capacity * MATRIX_FLOATS)

  /** Each draw's material, packed as [MaterialFloats] says. */
  public val materials: FloatArray = FloatArray(capacity * MaterialFloats.COUNT)
  internal val meshes = arrayOfNulls<MeshData>(capacity)

  /** How many draws the frame holds. */
  public var count: Int = 0
    private set

  /** Removes every draw. */
  public fun clearDraws() {
    meshes.fill(null, 0, count)
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

  /** The mesh of draw [index]. */
  public fun mesh(index: Int): MeshData = checkNotNull(meshes[index]) { "No draw $index" }

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
  }
}

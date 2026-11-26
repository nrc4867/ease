package dev.chieppa.ease.gltf

import dev.chieppa.ease.memory.MappedRegion

/** The JSON text and the optional binary chunk of a GLB container. */
public class GlbChunks(public val json: String, public val binary: MappedRegion?)

/**
 * The binary glTF container: a 12-byte header (`glTF`, version 2, total length), a JSON chunk, then
 * an optional BIN chunk, every length little-endian.
 */
public object Glb {
  private const val MAGIC = 0x46546C67L
  private const val JSON_CHUNK = 0x4E4F534AL
  private const val BIN_CHUNK = 0x004E4942L
  private const val VERSION = 2L
  private const val HEADER_BYTES = 12L
  private const val CHUNK_HEADER_BYTES = 8L
  private const val LENGTH_AT = 8L

  /** Whether [bytes] start with the GLB magic. */
  public fun isGlb(bytes: ByteArray): Boolean =
    bytes.size >= HEADER_BYTES && MappedRegion.of(bytes, "glb").uint32(0) == MAGIC

  /** The chunks of [bytes]; throws [GltfException] naming `glb` when the container is broken. */
  public fun split(bytes: ByteArray, name: String): GlbChunks {
    val region = MappedRegion.of(bytes, name)
    if (!isGlb(bytes)) fail("glb", "$name does not start with the GLB magic")
    val version = region.uint32(4)
    if (version != VERSION) fail("glb", "version $version is not 2")
    val length = region.uint32(LENGTH_AT)
    if (length > bytes.size) {
      fail("glb", "declares $length bytes, but the file has ${bytes.size}")
    }
    val json = chunk(region, HEADER_BYTES, length, JSON_CHUNK, "JSON")
    val jsonText = String(json.copyBytes(0, json.byteSize.toInt()), Charsets.UTF_8)
    val next = HEADER_BYTES + CHUNK_HEADER_BYTES + json.byteSize
    val binary =
      if (next + CHUNK_HEADER_BYTES <= length) chunk(region, next, length, BIN_CHUNK, "BIN")
      else null
    return GlbChunks(jsonText, binary)
  }

  private fun chunk(
    region: MappedRegion,
    at: Long,
    end: Long,
    type: Long,
    label: String,
  ): MappedRegion {
    if (at + CHUNK_HEADER_BYTES > end) fail("glb", "the $label chunk is missing")
    val length = region.uint32(at)
    val found = region.uint32(at + Int.SIZE_BYTES)
    if (found != type) fail("glb", "expected the $label chunk at byte $at")
    if (at + CHUNK_HEADER_BYTES + length > end) {
      fail("glb", "the $label chunk runs past the end of the file")
    }
    return region.region(at + CHUNK_HEADER_BYTES, length)
  }
}

package dev.chieppa.ease.gltf.json

import dev.chieppa.ease.annotations.Doc
import kotlinx.serialization.Serializable

/** A buffer: [byteLength] bytes at [uri], or the GLB container's binary chunk when it has none. */
@Doc("A buffer: byteLength bytes at uri, or the GLB container's binary chunk when it has none.")
@Serializable
public class BufferJson(
  @Doc("The glTF property byteLength.") public val byteLength: Long,
  @Doc("The glTF property uri.") public val uri: String? = null,
  @Doc("The glTF property name.") public val name: String? = null,
)

/** A slice of a buffer; [byteStride] is set for interleaved vertex data. */
@Doc("A slice of a buffer; byteStride is set for interleaved vertex data.")
@Serializable
public class BufferViewJson(
  @Doc("The glTF property buffer.") public val buffer: Int,
  @Doc("The glTF property byteLength.") public val byteLength: Long,
  @Doc("The glTF property byteOffset.") public val byteOffset: Long = 0,
  @Doc("The glTF property byteStride.") public val byteStride: Int? = null,
  @Doc("The glTF property target.") public val target: Int? = null,
  @Doc("The glTF property name.") public val name: String? = null,
)

/**
 * A typed view of a buffer view: [count] elements of [type] (`SCALAR`, `VEC2` to `VEC4`, `MAT2` to
 * `MAT4`) made of [componentType] numbers. With no [bufferView] the elements start as zeros, and
 * [sparse] replaces some of them.
 */
@Doc(
  "A typed view of a buffer view: count elements of type (SCALAR, VEC2 to VEC4, MAT2 to " +
    "MAT4) made of componentType numbers. With no bufferView the elements start as zeros, and" +
    " sparse replaces some of them."
)
@Serializable
@Suppress("LongParameterList") // The glTF object's properties, as the specification names them.
public class AccessorJson(
  @Doc("The glTF property componentType.") public val componentType: Int,
  @Doc("The glTF property count.") public val count: Int,
  @Doc("The glTF property type.") public val type: String,
  @Doc("The glTF property bufferView.") public val bufferView: Int? = null,
  @Doc("The glTF property byteOffset.") public val byteOffset: Long = 0,
  @Doc("The glTF property normalized.") public val normalized: Boolean = false,
  @Doc("The glTF property sparse.") public val sparse: SparseJson? = null,
  @Doc("The glTF property min.") public val min: List<Float>? = null,
  @Doc("The glTF property max.") public val max: List<Float>? = null,
  @Doc("The glTF property name.") public val name: String? = null,
)

/** The elements of an accessor that differ from its base values: [count] indices and values. */
@Doc("The elements of an accessor that differ from its base values: count indices and values.")
@Serializable
public class SparseJson(
  @Doc("The glTF property count.") public val count: Int,
  @Doc("The glTF property indices.") public val indices: SparseIndicesJson,
  @Doc("The glTF property values.") public val values: SparseValuesJson,
)

/** Where a sparse accessor's indices are, and their integer type. */
@Doc("Where a sparse accessor's indices are, and their integer type.")
@Serializable
public class SparseIndicesJson(
  @Doc("The glTF property bufferView.") public val bufferView: Int,
  @Doc("The glTF property componentType.") public val componentType: Int,
  @Doc("The glTF property byteOffset.") public val byteOffset: Long = 0,
)

/** Where a sparse accessor's replacement values are. */
@Doc("Where a sparse accessor's replacement values are.")
@Serializable
public class SparseValuesJson(
  @Doc("The glTF property bufferView.") public val bufferView: Int,
  @Doc("The glTF property byteOffset.") public val byteOffset: Long = 0,
)

package dev.chieppa.ease.sdf.gpu

import dev.chieppa.ease.memory.NativeWriter
import dev.chieppa.ease.sdf.SdfConstants.ROW_FLOATS
import dev.chieppa.ease.sdf.field.DistanceField
import java.lang.foreign.MemorySegment

/**
 * [distanceField]'s program for the GPU: [wgsl] defines `sdf_field(p: vec3f) -> f32`, the same
 * program [DistanceField.distance] runs, after the distance functions it calls. It reads its
 * parameters from `sdf_nodes`, an `array<SdfNode>` the shader declares and binds, filled by
 * [writeNodes].
 */
public class SdfShader(public val distanceField: DistanceField) {
  /** The WGSL of `sdf_field`, after the distance functions it calls. */
  public val wgsl: String = SdfWgsl.field(distanceField.program)

  /** How many [SdfNode]s [writeNodes] writes. */
  public val nodeCount: Int
    get() = distanceField.program.nodeCount

  /** Bytes [writeNodes] writes. */
  public val nodeBytes: Long
    get() = nodeCount * SdfNodeWriter.STRUCT_SIZE

  /** Writes every node's parameters into [segment], from its start, as `array<SdfNode>`. */
  public fun writeNodes(segment: MemorySegment) {
    require(segment.byteSize() >= nodeBytes) {
      "${segment.byteSize()} bytes cannot hold $nodeCount nodes"
    }
    val program = distanceField.program
    val writer = NativeWriter().begin(segment)
    val rows =
      listOf(
        SdfNodeWriter::inverse0,
        SdfNodeWriter::inverse1,
        SdfNodeWriter::inverse2,
        SdfNodeWriter::values,
      )
    for (node in 0 until nodeCount) {
      rows.forEachIndexed { row, write ->
        val at = row * ROW_FLOATS
        write(
          writer,
          program.parameter(node, at),
          program.parameter(node, at + 1),
          program.parameter(node, at + 2),
          program.parameter(node, at + ROW_FLOATS - 1),
        )
      }
      writer.next(SdfNodeWriter.STRUCT_SIZE)
    }
  }
}

package dev.chieppa.ease.sdf.mesh

import dev.chieppa.ease.gpu.mesh.ComputedMesh
import dev.chieppa.ease.memory.wgslDeclarations
import dev.chieppa.ease.sdf.field.DistanceField
import dev.chieppa.ease.sdf.field.SdfNodeWriter

/** The WGSL of each surface nets kernel: the structs and helpers it needs, then its own code. */
internal object SurfaceNetsShaders {
  private val structs = wgslDeclarations(listOf(SurfaceNetsGridWriter, SdfNodeWriter))
  private val constants = "const SN_NORMAL_STEP: f32 = ${SurfaceNets.NORMAL_STEP};\n"

  fun sample(field: DistanceField): String =
    join(field.wgsl, read("edges.wgsl"), read("positions.wgsl"), read("sample.wgsl"))

  fun classify(): String = join(read("edges.wgsl"), read("classify.wgsl"))

  fun scanBlocks(): String = join(read("scan_blocks.wgsl"))

  fun scanTotals(): String = join(read("scan_totals.wgsl"))

  fun emit(field: DistanceField): String =
    join(
      field.wgsl,
      read("edges.wgsl"),
      read("positions.wgsl"),
      ComputedMesh.WRITE_VERTEX_WGSL,
      read("emit.wgsl"),
    )

  private fun join(vararg parts: String): String =
    (listOf(structs, constants, read("common.wgsl")) + parts).joinToString("\n")

  private fun read(name: String): String =
    checkNotNull(SurfaceNetsShaders::class.java.getResource(name)) { name }.readText()
}

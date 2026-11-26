package dev.chieppa.ease.sdf.gpu

import dev.chieppa.ease.gpu.mesh.ComputedMesh
import dev.chieppa.ease.memory.wgslDeclarations
import dev.chieppa.ease.sdf.mesh.SurfaceNets

/**
 * The WGSL of each surface nets kernel: the structs and helpers it needs, then its own code. The
 * constants the kernels share are written from Kotlin's, so both sides use the same numbers.
 */
internal object SurfaceNetsShaders {
  /** Invocations in a workgroup, and corners in a scanned block. */
  const val WORKGROUP = 256

  private val structs = wgslDeclarations(listOf(SurfaceNetsGridWriter, SdfNodeWriter))
  private val constants =
    "const SN_WORKGROUP: u32 = ${WORKGROUP}u;\n" +
      "const SN_NORMAL_STEP: f32 = ${SurfaceNets.NORMAL_STEP};\n"

  fun sample(field: SdfShader): String =
    join(field.wgsl, read("edges.wgsl"), read("positions.wgsl"), read("sample.wgsl"))

  fun classify(): String = join(read("edges.wgsl"), read("classify.wgsl"))

  fun scanBlocks(): String = join(read("scan_blocks.wgsl"))

  fun scanTotals(): String = join(read("scan_totals.wgsl"))

  fun emit(field: SdfShader): String =
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

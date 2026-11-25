package dev.chieppa.ease.sdf.field

/**
 * Writes an [SdfProgram] as WGSL: the distance functions of `sdf.wgsl`, then `sdf_field`, one `let`
 * per instruction in the program's order. The text depends only on the tree's structure; sizes,
 * blends and transforms are read from `sdf_nodes`, so changing them needs no new shader.
 */
internal object SdfWgsl {
  private val library: String =
    checkNotNull(SdfWgsl::class.java.getResource("sdf.wgsl")) { "sdf.wgsl" }.readText()

  fun field(program: SdfProgram): String = buildString {
    append(library)
    append("\nfn sdf_field(p0: vec3f) -> f32 {\n")
    for (instruction in 0 until program.size) {
      append("  ").append(line(program, instruction)).append('\n')
    }
    append("  return d").append(program.result).append(";\n}\n")
  }

  private fun line(program: SdfProgram, instruction: Int): String {
    val target = program.target(instruction)
    val first = program.first(instruction)
    val second = program.second(instruction)
    val node = "sdf_nodes[${program.node(instruction)}u]"
    return when (val op = program.op(instruction)) {
      SdfOps.TRANSFORM -> "let p$target = sdf_transform(p$first, $node);"
      SdfOps.SCALE -> "let d$target = d$first * $node.values.x;"
      SdfOps.UNION,
      SdfOps.SUBTRACT,
      SdfOps.INTERSECT -> "let d$target = ${join(op)}(d$first, d$second, $node.values.x);"
      else -> "let d$target = ${primitive(op)}(p$first, $node.values);"
    }
  }

  private fun join(op: Int): String =
    when (op) {
      SdfOps.UNION -> "sdf_union"
      SdfOps.SUBTRACT -> "sdf_subtract"
      else -> "sdf_intersect"
    }

  private fun primitive(op: Int): String =
    when (op) {
      SdfOps.SPHERE -> "sdf_sphere"
      SdfOps.BOX -> "sdf_box"
      SdfOps.ROUNDED_BOX -> "sdf_rounded_box"
      SdfOps.CAPSULE -> "sdf_capsule"
      SdfOps.CYLINDER -> "sdf_cylinder"
      SdfOps.PLANE -> "sdf_plane"
      SdfOps.TORUS -> "sdf_torus"
      else -> error("Opcode $op is not a primitive")
    }
}

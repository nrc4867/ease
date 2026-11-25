package dev.chieppa.ease.codegen.output

import dev.chieppa.ease.codegen.model.GpuStructLayout

/** The WGSL declaration of a laid-out GPU struct, one field a line, each with its attributes. */
internal object WgslStructSource {
  fun of(layout: GpuStructLayout): String = buildString {
    appendLine("struct ${layout.model.wgslName} {")
    layout.fields.forEach { field ->
      append("  ")
      field.alignAttribute?.let { append("@align($it) ") }
      field.sizeAttribute?.let { append("@size($it) ") }
      appendLine("${field.field.name}: ${field.wgslType},")
    }
    appendLine("}")
  }
}

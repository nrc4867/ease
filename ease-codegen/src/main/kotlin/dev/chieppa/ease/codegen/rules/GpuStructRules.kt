package dev.chieppa.ease.codegen.rules

import dev.chieppa.ease.codegen.model.ClassVisibility
import dev.chieppa.ease.codegen.model.GpuFieldType
import dev.chieppa.ease.codegen.model.GpuStructModel
import dev.chieppa.ease.codegen.output.GpuWriterSource

/** What makes a [GpuStructModel] impossible to lay out, each problem naming its field if any. */
internal object GpuStructRules {
  /** Every problem with [model], empty when it can be laid out. */
  fun problems(model: GpuStructModel): List<Problem> = buildList {
    if (model.fields.isEmpty()) {
      add(Problem(null, "a GPU struct needs at least one constructor property"))
    }
    if (model.visibility == ClassVisibility.HIDDEN) {
      add(Problem(null, "a GPU struct and every class around it must be public or internal"))
    }
    if (WgslWords.refuses(model.wgslName)) {
      add(Problem(null, "a GPU struct may not be named ${model.wgslName}, a word WGSL reserves"))
    }
    model.fields.forEach { field ->
      val type = field.type
      if (type is GpuFieldType.Unsupported) {
        add(
          Problem(
            field.name,
            "a GPU struct field is a Float, Int, UInt, Vec2, Vec3, Vec4, Mat4 or another " +
              "@GpuStruct, not ${type.typeName}",
          )
        )
      }
      if (GpuWriterSource.constantName(field.name) in GpuWriterSource.RESERVED_CONSTANTS) {
        add(Problem(field.name, "a GPU struct field may not be named ${field.name}"))
      }
      if (WgslWords.refuses(field.name)) {
        add(Problem(field.name, "a GPU struct field may not be named ${field.name}, a WGSL word"))
      }
      if (type is GpuFieldType.Struct) addAll(nestedProblems(field.name, type.model))
    }
  }

  private fun nestedProblems(name: String, model: GpuStructModel): List<Problem> =
    problems(model).map { Problem(name, "nested ${model.wgslName}: ${it.message}") }
}

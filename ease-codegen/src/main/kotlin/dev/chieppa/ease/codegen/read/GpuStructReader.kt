package dev.chieppa.ease.codegen.read

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import dev.chieppa.ease.codegen.model.GpuFieldModel
import dev.chieppa.ease.codegen.model.GpuFieldType
import dev.chieppa.ease.codegen.model.GpuPrimitive
import dev.chieppa.ease.codegen.model.GpuStructModel

/**
 * Reads a `@GpuStruct` class into a [GpuStructModel]: one field per property of its primary
 * constructor, in order, a nested `@GpuStruct` read the same way. A struct that holds itself, at
 * any depth, reads as an unsupported field.
 */
internal class GpuStructReader {
  fun read(declaration: KSClassDeclaration): GpuStructModel = read(declaration, emptySet())

  private fun read(declaration: KSClassDeclaration, outer: Set<String>): GpuStructModel {
    val packageName = declaration.packageName.asString()
    val qualified = declaration.qualifiedName?.asString() ?: declaration.simpleName.asString()
    val parameters = declaration.primaryConstructor?.parameters.orEmpty()
    val fields =
      parameters
        .filter { it.isVal || it.isVar }
        .map { parameter ->
          val name = parameter.name?.asString().orEmpty()
          GpuFieldModel(name, fieldType(parameter.type.resolve(), outer + qualified))
        }
    return GpuStructModel(
      packageName = packageName,
      className = qualified.removePrefix("$packageName."),
      fields = fields,
      visibility = classVisibility(declaration),
    )
  }

  private fun fieldType(type: KSType, outer: Set<String>): GpuFieldType {
    val typeName = type.qualifiedName() ?: type.toString()
    val primitive = GpuPrimitive.of(typeName)
    val struct = type.declaration as? KSClassDeclaration
    return when {
      type.isMarkedNullable -> GpuFieldType.Unsupported("$typeName?")
      primitive != null -> GpuFieldType.Plain(primitive)
      struct == null || struct.annotation(AnnotationNames.GPU_STRUCT) == null ->
        GpuFieldType.Unsupported(typeName)
      typeName in outer -> GpuFieldType.Unsupported("$typeName, which holds itself")
      else -> GpuFieldType.Struct(read(struct, outer))
    }
  }
}

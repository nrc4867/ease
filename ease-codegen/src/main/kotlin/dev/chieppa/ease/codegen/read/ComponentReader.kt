package dev.chieppa.ease.codegen.read

import com.google.devtools.ksp.getConstructors
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.isPublic
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import dev.chieppa.ease.codegen.model.ComponentModel
import dev.chieppa.ease.codegen.model.FieldModel

/**
 * Reads an annotated class into a [ComponentModel]: its names, whether it is a Fleks component with
 * a type companion and a constructor without arguments, and one [FieldModel] for every public `var`
 * with a backing field, in declaration order, each run through [optionReaders].
 */
internal class ComponentReader(private val optionReaders: List<FieldOptionReader>) {
  fun read(declaration: KSClassDeclaration): ComponentModel {
    val packageName = declaration.packageName.asString()
    val qualified = declaration.qualifiedName?.asString() ?: declaration.simpleName.asString()
    val className = qualified.removePrefix("$packageName.")
    return ComponentModel(
      packageName = packageName,
      className = className,
      serialName = declaration.serialNameAnnotation() ?: qualified,
      fields = replicatedProperties(declaration).map { field(it) }.toList(),
      visibility = classVisibility(declaration),
      isComponent = declaration.implements(COMPONENT),
      hasTypeCompanion = companionOf(declaration)?.implements(COMPONENT_TYPE) ?: false,
      hasNoArgumentConstructor =
        declaration.getConstructors().any { constructor ->
          isReachable(constructor) && constructor.parameters.all { it.hasDefault }
        },
    )
  }

  private fun replicatedProperties(
    declaration: KSClassDeclaration
  ): Sequence<KSPropertyDeclaration> =
    declaration.getDeclaredProperties().filter { property ->
      property.isMutable && property.hasBackingField && property.isPublic()
    }

  private fun field(property: KSPropertyDeclaration): FieldModel {
    val type = property.type.resolve()
    val typeName = type.qualifiedName() ?: type.toString()
    val plain =
      FieldModel(
        property.simpleName.asString(),
        typeName,
        type.isMarkedNullable,
        settable = isSettable(property),
      )
    return optionReaders.fold(plain) { field, reader -> reader.read(property, field) }
  }

  private fun companionOf(declaration: KSClassDeclaration): KSClassDeclaration? =
    declaration.declarations.filterIsInstance<KSClassDeclaration>().firstOrNull {
      it.isCompanionObject
    }

  private fun KSClassDeclaration.implements(qualifiedName: String): Boolean =
    superTypes.any { reference ->
      reference.resolve().qualifiedName() == qualifiedName
    }

  private companion object {
    const val COMPONENT = "com.github.quillraven.fleks.Component"
    const val COMPONENT_TYPE = "com.github.quillraven.fleks.ComponentType"
  }
}

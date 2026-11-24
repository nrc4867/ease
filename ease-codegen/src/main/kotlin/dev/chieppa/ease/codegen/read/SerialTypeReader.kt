package dev.chieppa.ease.codegen.read

import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Modifier
import dev.chieppa.ease.codegen.model.DescribedType
import dev.chieppa.ease.codegen.model.SerialField
import dev.chieppa.ease.codegen.model.SerialType
import dev.chieppa.ease.codegen.model.TypeKind
import dev.chieppa.ease.codegen.model.TypeRef
import dev.chieppa.ease.codegen.model.references

/**
 * Reads `@Serializable` classes into [SerialType]s: their serial names, kinds, docs, ranges and one
 * [SerialField] for every property the serialization plugin writes, in declaration order. A class a
 * field refers to is read the same way, wherever it is declared, so [describe] gives a type with
 * everything reachable from it.
 */
internal class SerialTypeReader {
  private val discovered = mutableMapOf<String, KSClassDeclaration>()
  private val types = TypeRefReader(discovered)
  private val read = mutableMapOf<String, SerialType>()

  /** [declaration] with every type reachable from it. */
  fun describe(declaration: KSClassDeclaration): DescribedType {
    val root = readOnce(declaration)
    val reachable = linkedMapOf(root.serialName to root)
    val pending = ArrayDeque(root.references())
    while (pending.isNotEmpty()) {
      val name = pending.removeFirst()
      if (name in reachable) continue
      val type = readOnce(checkNotNull(discovered[name]))
      reachable[name] = type
      pending.addAll(type.references())
    }
    return DescribedType(root, reachable)
  }

  private fun readOnce(declaration: KSClassDeclaration): SerialType =
    read.getOrPut(declaration.serialName()) { readClass(declaration) }

  private fun readClass(declaration: KSClassDeclaration): SerialType {
    val serialName = declaration.serialName()
    val doc = declaration.doc()
    val custom = declaration.customSerializer()
    return when {
      custom != null -> customOf(serialName, doc, custom)
      declaration.classKind == ClassKind.ENUM_CLASS -> enumOf(declaration, serialName, doc)
      Modifier.SEALED in declaration.modifiers -> sealedOf(declaration, serialName, doc)
      else ->
        SerialType(
          serialName,
          if (declaration.isValueClass()) TypeKind.VALUE else TypeKind.CLASS,
          doc,
          fields = fieldsOf(declaration),
          discriminated = declaration.hasPolymorphicBase(),
        )
    }
  }

  /** A class with its own serializer is as that serializer writes it, which only it knows. */
  private fun customOf(serialName: String, doc: String?, serializer: KSType) =
    SerialType(
      serialName,
      TypeKind.VALUE,
      doc,
      fields = listOf(SerialField("value", types.readSerializedWith(serializer))),
    )

  private fun enumOf(declaration: KSClassDeclaration, serialName: String, doc: String?) =
    SerialType(
      serialName,
      TypeKind.ENUM,
      doc,
      values =
        declaration.declarations
          .filterIsInstance<KSClassDeclaration>()
          .filter { it.classKind == ClassKind.ENUM_ENTRY }
          .map { it.serialNameAnnotation() ?: it.simpleName.asString() }
          .toList(),
    )

  private fun sealedOf(declaration: KSClassDeclaration, serialName: String, doc: String?) =
    SerialType(
      serialName,
      TypeKind.SEALED,
      doc,
      subtypes =
        declaration
          .getSealedSubclasses()
          .filter { it.isSerializable() }
          .map { subtype -> subtype.serialName().also { discovered.putIfAbsent(it, subtype) } }
          .toList(),
      discriminated = declaration.hasPolymorphicBase(),
    )

  /** The properties of the serializable superclass first, as the serialization plugin writes. */
  private fun fieldsOf(declaration: KSClassDeclaration): List<SerialField> =
    serializableSuperclass(declaration)?.let { fieldsOf(it) }.orEmpty() + ownFields(declaration)

  private fun serializableSuperclass(declaration: KSClassDeclaration): KSClassDeclaration? =
    declaration.superTypes
      .map { it.resolve().declaration }
      .filterIsInstance<KSClassDeclaration>()
      .firstOrNull { it.classKind == ClassKind.CLASS && it.isSerializable() }

  private fun ownFields(declaration: KSClassDeclaration): List<SerialField> {
    val constructorParameters = declaration.primaryConstructor?.parameters.orEmpty()
    val parameters = constructorParameters.associateBy { it.name }
    return declaration
      .getDeclaredProperties()
      .filter { it.isSerialized() }
      .map { property ->
        val parameter = parameters[property.simpleName]
        SerialField(
          name = property.serialNameAnnotation() ?: property.simpleName.asString(),
          type = typeOf(property),
          doc = property.doc(),
          range = property.range(),
          optional = parameter?.hasDefault ?: true,
          propertyName = property.simpleName.asString(),
        )
      }
      .toList()
  }

  private fun typeOf(property: KSPropertyDeclaration): TypeRef {
    property.customSerializer()?.let {
      return types.readSerializedWith(it)
    }
    return types.read(property.type.resolve())
  }
}

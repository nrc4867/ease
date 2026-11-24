package dev.chieppa.ease.codegen.output

import dev.chieppa.ease.codegen.model.ClassVisibility
import dev.chieppa.ease.codegen.model.ComponentModel
import dev.chieppa.ease.codegen.model.FieldKind

/**
 * The Kotlin source of a model's `ReplicatedSerializer`: an object in the class's package that
 * reads and writes the component's fields directly, so nothing reflects or allocates at runtime.
 * Its wire form matches the schema-driven serializers' field for field.
 */
internal object SerializerSource {
  private val imports =
    listOf(
      "com.github.quillraven.fleks.ComponentType",
      "com.github.quillraven.fleks.Entity",
      "com.github.quillraven.fleks.World",
      "dev.chieppa.ease.ecs.replication.BitReader",
      "dev.chieppa.ease.ecs.replication.BitWriter",
      "dev.chieppa.ease.ecs.replication.ReplicatedFields",
      "dev.chieppa.ease.ecs.replication.ReplicatedSerializer",
    )

  fun of(model: ComponentModel): String = buildString {
    appendLine("// Written by ease-codegen from ${model.className}; changes here are lost.")
    if (model.packageName.isNotEmpty()) {
      appendLine("package ${model.packageName}")
      appendLine()
    }
    imports.forEach { appendLine("import $it") }
    appendLine()
    appendHeader(model)
    appendChangedFields(model)
    appendNonFinite(model)
    appendDiffers(model)
    appendWrite(model)
    appendRead(model)
    appendCopy(model)
    appendLine("}")
  }

  private fun StringBuilder.appendHeader(model: ComponentModel) {
    val type = model.className
    val visibility = if (model.visibility == ClassVisibility.PUBLIC) "public" else "internal"
    appendLine("/** The replicated serializer of [$type]. */")
    appendLine("$visibility object ${model.serializerName} : ReplicatedSerializer<$type> {")
    appendLine("  override val name: String = \"${model.serialName}\"")
    appendLine("  override val type: ComponentType<$type> = $type")
    appendLine("  override val fieldCount: Int = ${model.fields.size}")
    val names = model.fields.joinToString { "\"${it.name}\"" }
    appendLine("  override val fieldNames: List<String> = listOf($names)")
    appendLine("  override val interpolatedFields: Int = ${mask(model.interpolatedFields)}")
    appendLine("  override val ownerFields: Int = ${mask(model.ownerFields)}")
    appendLine("  override val othersFields: Int = ${mask(model.othersFields)}")
    appendLine()
    appendLine("  override fun create(): $type = $type()")
    appendLine()
    appendLine("  override fun componentOf(world: World, entity: Entity): $type =")
    appendLine("    with(world) { entity[$type] }")
  }

  private fun StringBuilder.appendChangedFields(model: ComponentModel) {
    appendLine()
    appendLine("  override fun changedFields(value: ${model.className}, ")
    appendLine("      baseline: ${model.className}): Int {")
    appendLine("    var mask = 0")
    model.fields.forEachIndexed { index, field ->
      appendLine("    if (${FieldSource.wireDiffers(field)}) mask = mask or (1 shl $index)")
    }
    appendLine("    return mask")
    appendLine("  }")
  }

  private fun StringBuilder.appendNonFinite(model: ComponentModel) {
    appendLine()
    appendLine("  override fun nonFiniteField(value: ${model.className}, fields: Int): Int {")
    model.fields.forEachIndexed { index, field ->
      if (field.kind == FieldKind.FLOAT || field.kind == FieldKind.QUANTIZED_FLOAT) {
        val named = "fields and (1 shl $index) != 0"
        appendLine("    if ($named && !value.${field.name}.isFinite()) return $index")
      }
    }
    appendLine("    return -1")
    appendLine("  }")
  }

  private fun StringBuilder.appendDiffers(model: ComponentModel) {
    appendLine()
    appendLine("  override fun differs(a: ${model.className}, b: ${model.className}): Boolean {")
    model.fields.forEach { appendLine("    if (${FieldSource.exactlyDiffers(it)}) return true") }
    appendLine("    return false")
    appendLine("  }")
  }

  private fun StringBuilder.appendWrite(model: ComponentModel) {
    appendLine()
    appendLine("  override fun write(writer: BitWriter, value: ${model.className}, fields: Int) {")
    appendLine("    val mask = fields and ${mask(ComponentMasks.all(model.fields.size))}")
    appendLine("    writer.writeBits(mask, ${model.fields.size})")
    model.fields.forEachIndexed { index, field ->
      appendLine("    if (mask and (1 shl $index) != 0) ${FieldSource.write(field)}")
    }
    appendLine("  }")
  }

  private fun StringBuilder.appendRead(model: ComponentModel) {
    appendLine()
    appendLine("  override fun read(reader: BitReader, target: ${model.className}) {")
    appendLine("    val mask = reader.readBits(${model.fields.size})")
    model.fields.forEachIndexed { index, field ->
      val read = FieldSource.read(field)
      appendLine("    if (mask and (1 shl $index) != 0) target.${field.name} = $read")
    }
    appendLine("  }")
  }

  private fun StringBuilder.appendCopy(model: ComponentModel) {
    appendLine()
    appendLine("  override fun copy(from: ${model.className}, to: ${model.className}) {")
    model.fields.forEach { appendLine("    to.${it.name} = from.${it.name}") }
    appendLine("  }")
  }

  private fun mask(value: Int): String = ComponentMasks.literal(value)
}

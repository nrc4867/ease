package dev.chieppa.ease.ecs.replication

import com.github.quillraven.fleks.ComponentType
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World

/** A [ReplicatedSchema]'s serializer: the generated serializers' wire form, led by the schema. */
internal class SchemaSerializer(private val schema: ReplicatedSchema) :
  ReplicatedSerializer<SchemaComponent> {
  private val fields = schema.fields.toTypedArray()

  override val name: String = schema.name
  override val type: ComponentType<SchemaComponent>
    get() = schema.type

  override val fieldCount: Int = fields.size
  override val interpolatedFields: Int = maskOf { it.interpolated }
  override val ownerFields: Int = maskOf { it.audience != Audience.ALL_BUT_OWNER }
  override val othersFields: Int = maskOf { it.audience != Audience.OWNER_ONLY }

  override fun create(): SchemaComponent = schema.component()

  override fun componentOf(world: World, entity: Entity): SchemaComponent =
    with(world) { entity[schema.type] }

  override fun changedFields(value: SchemaComponent, baseline: SchemaComponent): Int {
    var mask = 0
    for (index in fields.indices) {
      if (wireValue(value, index) != wireValue(baseline, index)) mask = mask or (1 shl index)
    }
    return mask
  }

  override fun differs(a: SchemaComponent, b: SchemaComponent): Boolean {
    var differs = false
    for (index in fields.indices) {
      val sameFloat = ReplicatedFields.sameFloat(a.getFloat(index), b.getFloat(index))
      if (!sameFloat || a.getInt(index) != b.getInt(index)) differs = true
    }
    return differs
  }

  override fun write(writer: BitWriter, value: SchemaComponent, fields: Int) {
    val mask = fields and ReplicatedFields.allFields(fieldCount)
    writer.writeBits(mask, fieldCount)
    for (index in this.fields.indices) {
      if (mask and (1 shl index) != 0) writeField(writer, value, index)
    }
  }

  override fun read(reader: BitReader, target: SchemaComponent) {
    val mask = reader.readBits(fieldCount)
    for (index in fields.indices) {
      if (mask and (1 shl index) != 0) readField(reader, target, index)
    }
  }

  override fun copy(from: SchemaComponent, to: SchemaComponent) {
    for (index in fields.indices) {
      to.setFloat(index, from.getFloat(index))
      to.setInt(index, from.getInt(index))
    }
  }

  /** The field at [index] as the bits it takes on the wire. */
  private fun wireValue(component: SchemaComponent, index: Int): Int {
    val field = fields[index]
    return when (field.kind) {
      SchemaFieldKind.FLOAT -> component.getFloat(index).toRawBits()
      SchemaFieldKind.QUANTIZED_FLOAT ->
        checkNotNull(field.quantization).stepOf(component.getFloat(index))
      SchemaFieldKind.INT,
      SchemaFieldKind.BOOLEAN -> component.getInt(index)
    }
  }

  private fun writeField(writer: BitWriter, value: SchemaComponent, index: Int) {
    val field = fields[index]
    val number = value.getFloat(index)
    when (field.kind) {
      SchemaFieldKind.FLOAT -> ReplicatedFields.writeFloat(writer, number)
      SchemaFieldKind.QUANTIZED_FLOAT -> checkNotNull(field.quantization).write(writer, number)
      SchemaFieldKind.INT -> ReplicatedFields.writeInt(writer, value.getInt(index))
      SchemaFieldKind.BOOLEAN -> writer.writeBoolean(value.getBoolean(index))
    }
  }

  private fun readField(reader: BitReader, target: SchemaComponent, index: Int) {
    val field = fields[index]
    when (field.kind) {
      SchemaFieldKind.FLOAT -> target.setFloat(index, ReplicatedFields.readFloat(reader))
      SchemaFieldKind.QUANTIZED_FLOAT -> {
        val number = checkNotNull(field.quantization).read(reader)
        target.setFloat(index, number)
      }
      SchemaFieldKind.INT -> target.setInt(index, ReplicatedFields.readInt(reader))
      SchemaFieldKind.BOOLEAN -> target.setBoolean(index, reader.readBoolean())
    }
  }

  private inline fun maskOf(selected: (SchemaField) -> Boolean): Int {
    var mask = 0
    fields.forEachIndexed { index, field -> if (selected(field)) mask = mask or (1 shl index) }
    return mask
  }
}

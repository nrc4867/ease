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
  override val fieldNames: List<String> = fields.map { it.name }
  override val interpolatedFields: Int = maskOf { it.interpolated }
  override val ownerFields: Int = maskOf { it.audience != Audience.ALL_BUT_OWNER }
  override val othersFields: Int = maskOf { it.audience != Audience.OWNER_ONLY }

  override fun create(): SchemaComponent = schema.component()

  override fun componentOf(world: World, entity: Entity): SchemaComponent =
    with(world) { entity[schema.type] }

  override fun changedFields(value: SchemaComponent, baseline: SchemaComponent): Int {
    var mask = 0
    for (index in fields.indices) {
      if (fields[index].wireValue(value, index) != fields[index].wireValue(baseline, index))
        mask = mask or (1 shl index)
    }
    return mask
  }

  override fun nonFiniteField(value: SchemaComponent, fields: Int): Int {
    var found = -1
    for (index in this.fields.indices.reversed()) {
      val named = fields and (1 shl index) != 0
      val kind = this.fields[index].kind
      val float = kind == SchemaFieldKind.FLOAT || kind == SchemaFieldKind.QUANTIZED_FLOAT
      if (named && float && !value.getFloat(index).isFinite()) found = index
    }
    return found
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
      if (mask and (1 shl index) != 0) this.fields[index].write(writer, value, index)
    }
  }

  override fun read(reader: BitReader, target: SchemaComponent) {
    val mask = reader.readBits(fieldCount)
    for (index in fields.indices) {
      if (mask and (1 shl index) != 0) fields[index].read(reader, target, index)
    }
  }

  override fun copy(from: SchemaComponent, to: SchemaComponent) {
    for (index in fields.indices) {
      to.setFloat(index, from.getFloat(index))
      to.setInt(index, from.getInt(index))
    }
  }

  override fun interpolate(
    from: SchemaComponent,
    to: SchemaComponent,
    amount: Float,
    into: SchemaComponent,
  ) {
    copy(from, into)
    for (index in fields.indices) {
      if (!fields[index].interpolated) continue
      val start = from.getFloat(index)
      into.setFloat(index, start + (to.getFloat(index) - start) * amount)
    }
  }

  private inline fun maskOf(selected: (SchemaField) -> Boolean): Int {
    var mask = 0
    fields.forEachIndexed { index, field -> if (selected(field)) mask = mask or (1 shl index) }
    return mask
  }
}

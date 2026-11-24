package dev.chieppa.ease.ecs.replication

/** This field, at [index] of [component], as the bits it takes on the wire. */
internal fun SchemaField.wireValue(component: SchemaComponent, index: Int): Int {
  return when (kind) {
    SchemaFieldKind.FLOAT -> component.getFloat(index).toRawBits()
    SchemaFieldKind.QUANTIZED_FLOAT -> checkNotNull(quantization).stepOf(component.getFloat(index))
    SchemaFieldKind.INT,
    SchemaFieldKind.BOOLEAN -> component.getInt(index)
  }
}

/** Writes this field, at [index] of [value]. */
internal fun SchemaField.write(writer: BitWriter, value: SchemaComponent, index: Int) {
  val number = value.getFloat(index)
  when (kind) {
    SchemaFieldKind.FLOAT -> ReplicatedFields.writeFloat(writer, number)
    SchemaFieldKind.QUANTIZED_FLOAT -> checkNotNull(quantization).write(writer, number)
    SchemaFieldKind.INT -> ReplicatedFields.writeInt(writer, value.getInt(index))
    SchemaFieldKind.BOOLEAN -> writer.writeBoolean(value.getBoolean(index))
  }
}

/** Reads this field into [index] of [target]. */
internal fun SchemaField.read(reader: BitReader, target: SchemaComponent, index: Int) {
  when (kind) {
    SchemaFieldKind.FLOAT -> target.setFloat(index, ReplicatedFields.readFloat(reader))
    SchemaFieldKind.QUANTIZED_FLOAT -> {
      val number = checkNotNull(quantization).read(reader)
      target.setFloat(index, number)
    }
    SchemaFieldKind.INT -> target.setInt(index, ReplicatedFields.readInt(reader))
    SchemaFieldKind.BOOLEAN -> target.setBoolean(index, reader.readBoolean())
  }
}

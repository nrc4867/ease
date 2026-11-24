package dev.chieppa.ease.ecs.replication

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType

/**
 * A component of a [ReplicatedSchema]: one value per field, read and written by field index (see
 * [ReplicatedSchema.indexOf]). Values sit in primitive arrays, so access allocates nothing.
 */
public class SchemaComponent internal constructor(public val schema: ReplicatedSchema) :
  Component<SchemaComponent> {
  private val floats = FloatArray(schema.fields.size)
  private val ints = IntArray(schema.fields.size)

  override fun type(): ComponentType<SchemaComponent> = schema.type

  /** The float field at [index]. */
  public fun getFloat(index: Int): Float = floats[index]

  /** Sets the float field at [index]. */
  public fun setFloat(index: Int, value: Float) {
    floats[index] = value
  }

  /** The int field at [index]. */
  public fun getInt(index: Int): Int = ints[index]

  /** Sets the int field at [index]. */
  public fun setInt(index: Int, value: Int) {
    ints[index] = value
  }

  /** The boolean field at [index]. */
  public fun getBoolean(index: Int): Boolean = ints[index] != 0

  /** Sets the boolean field at [index]. */
  public fun setBoolean(index: Int, value: Boolean) {
    ints[index] = if (value) 1 else 0
  }
}

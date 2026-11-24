package dev.chieppa.ease.ecs.replication

import com.github.quillraven.fleks.ComponentType

/**
 * A replicated component type described at runtime, such as a script's component, rather than
 * declared as a Kotlin class: its [name], its [fields] in wire order, its own component [type] and
 * the [serializer] that follows the schema instead of generated code.
 */
public class ReplicatedSchema
internal constructor(public val name: String, public val fields: List<SchemaField>) {
  /** The component type of this schema's components. */
  public val type: ComponentType<SchemaComponent> = object : ComponentType<SchemaComponent>() {}

  /** Writes and reads this schema's components. */
  public val serializer: ReplicatedSerializer<SchemaComponent> = SchemaSerializer(this)

  private val indexByName = fields.withIndex().associate { (index, field) -> field.name to index }

  /** The index of the field named [field], for [SchemaComponent]'s accessors. */
  public fun indexOf(field: String): Int =
    indexByName[field] ?: throw IllegalArgumentException("$name has no field $field")

  /** A component of this schema with every field zero or false. */
  public fun component(): SchemaComponent = SchemaComponent(this)
}

/** Builds a [ReplicatedSchema] named [name] from the fields [fields] declares, in order. */
public fun replicatedSchema(name: String, fields: SchemaBuilder.() -> Unit): ReplicatedSchema =
  ReplicatedSchema(name, SchemaBuilder().apply(fields).build())

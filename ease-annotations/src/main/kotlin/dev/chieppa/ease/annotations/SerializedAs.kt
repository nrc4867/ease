package dev.chieppa.ease.annotations

import kotlin.reflect.KClass

/**
 * Marks a custom serializer as writing the same data as the `@Serializable` class [type], so the
 * generators describe a field serialized with it through that class.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
public annotation class SerializedAs(val type: KClass<*>)

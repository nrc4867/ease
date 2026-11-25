package dev.chieppa.ease.memory

/**
 * Declares a struct the GPU reads, from which ease-codegen writes, beside the class, an object
 * named after it with `Writer` appended: a [GpuLayout] holding each field's byte offset as a
 * constant, one write function per field taking a [NativeWriter], and the WGSL struct text shaders
 * include. Offsets follow WGSL's alignment rules, with the attributes a struct needs to be valid in
 * both the uniform and the storage address space, so Kotlin and the shaders cannot disagree.
 *
 * The class is a declaration only and is never built. Its constructor's properties, in order, are
 * the fields: `Float` (`f32`), `Int` (`i32`), `UInt` (`u32`), ease-math's `Vec2`, `Vec3` and `Vec4`
 * (`vec2f`, `vec3f`, `vec4f`), `Mat4` (`mat4x4f`, column-major), or another `@GpuStruct` class.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
public annotation class GpuStruct

/**
 * Vectors, quaternions, matrices, transforms and bounds.
 *
 * <p>Conventions: right-handed, Y up, column vectors. Matrices are stored column-major, the layout
 * WGSL expects, so element (row r, column c) of a {@code float[16]} is at index {@code 4 * c + r}.
 * Clip-space depth runs from 0 to 1, as in WebGPU.
 *
 * <p>The record types ({@code Vec3}, {@code Quat}, {@code Mat4}, ...) are immutable and allocate.
 * The holder classes ({@code Vec3s}, {@code Quats}, {@code Mat4s}, ...) operate on {@code float[]}
 * with offsets, allocate nothing, and accept a destination that aliases a source.
 *
 * <p>Trigonometry goes through {@code Trig} and {@code StrictMath}, and the rest uses only
 * correctly rounded operations, so every JVM computes the same bits: host and clients agree on what
 * they derive. Neither allocates on the paths the holder classes take.
 */
package dev.chieppa.ease.math;

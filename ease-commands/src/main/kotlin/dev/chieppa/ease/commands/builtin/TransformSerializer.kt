package dev.chieppa.ease.commands.builtin

import dev.chieppa.ease.annotations.Doc
import dev.chieppa.ease.annotations.SerializedAs
import dev.chieppa.ease.math.Quat
import dev.chieppa.ease.math.Transform
import dev.chieppa.ease.math.Vec3
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/** Writes a [Transform] as `{translation, rotation, scale}` with named float fields. */
@SerializedAs(TransformData::class)
internal object TransformSerializer : KSerializer<Transform> {
  override val descriptor: SerialDescriptor = TransformData.serializer().descriptor

  override fun serialize(encoder: Encoder, value: Transform) {
    encoder.encodeSerializableValue(TransformData.serializer(), TransformData.of(value))
  }

  override fun deserialize(decoder: Decoder): Transform =
    decoder.decodeSerializableValue(TransformData.serializer()).toTransform()
}

@Doc("A translation, a rotation and a scale.")
@Serializable
@SerialName("ease.Transform")
private class TransformData(
  @Doc("Position in world units.") val translation: Vec3Data,
  @Doc("Rotation as a quaternion.") val rotation: QuatData,
  @Doc("Scale along each axis; 1 leaves the size unchanged.") val scale: Vec3Data,
) {
  fun toTransform(): Transform = Transform(translation.toVec3(), rotation.toQuat(), scale.toVec3())

  companion object {
    fun of(transform: Transform): TransformData =
      TransformData(
        Vec3Data.of(transform.translation()),
        QuatData.of(transform.rotation()),
        Vec3Data.of(transform.scale()),
      )
  }
}

@Doc("Three floats along x, y and z.")
@Serializable
@SerialName("ease.Vec3")
private class Vec3Data(
  @Doc("The x component.") val x: Float,
  @Doc("The y component.") val y: Float,
  @Doc("The z component.") val z: Float,
) {
  fun toVec3(): Vec3 = Vec3(x, y, z)

  companion object {
    fun of(vector: Vec3): Vec3Data = Vec3Data(vector.x(), vector.y(), vector.z())
  }
}

@Doc("A rotation as a quaternion; 0, 0, 0, 1 is no rotation.")
@Serializable
@SerialName("ease.Quat")
private class QuatData(
  @Doc("The x component.") val x: Float,
  @Doc("The y component.") val y: Float,
  @Doc("The z component.") val z: Float,
  @Doc("The w component.") val w: Float,
) {
  fun toQuat(): Quat = Quat(x, y, z, w)

  companion object {
    fun of(rotation: Quat): QuatData =
      QuatData(rotation.x(), rotation.y(), rotation.z(), rotation.w())
  }
}

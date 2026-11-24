package dev.chieppa.ease.commands.builtin

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
internal object TransformSerializer : KSerializer<Transform> {
  override val descriptor: SerialDescriptor = TransformData.serializer().descriptor

  override fun serialize(encoder: Encoder, value: Transform) {
    encoder.encodeSerializableValue(TransformData.serializer(), TransformData.of(value))
  }

  override fun deserialize(decoder: Decoder): Transform =
    decoder.decodeSerializableValue(TransformData.serializer()).toTransform()
}

@Serializable
@SerialName("ease.Transform")
private class TransformData(
  val translation: Vec3Data,
  val rotation: QuatData,
  val scale: Vec3Data,
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

@Serializable
private class Vec3Data(val x: Float, val y: Float, val z: Float) {
  fun toVec3(): Vec3 = Vec3(x, y, z)

  companion object {
    fun of(vector: Vec3): Vec3Data = Vec3Data(vector.x(), vector.y(), vector.z())
  }
}

@Serializable
private class QuatData(val x: Float, val y: Float, val z: Float, val w: Float) {
  fun toQuat(): Quat = Quat(x, y, z, w)

  companion object {
    fun of(rotation: Quat): QuatData =
      QuatData(rotation.x(), rotation.y(), rotation.z(), rotation.w())
  }
}

package dev.chieppa.ease.render.assets.animation

import dev.chieppa.ease.assets.Guid
import dev.chieppa.ease.assets.loading.AssetLoader
import kotlin.reflect.KClass

private const val SKIN_MAGIC = 0x4E4B5345 // "ESKN"
private const val CLIP_MAGIC = 0x50494C45 // "ELIP"
private const val VERSION = 1
private const val NONE = ""

/**
 * The cooked skin file (`.skin`), little-endian: magic `ESKN`, version, the joint count, each
 * joint's node id and name, the skeleton root's id (empty for none), then the inverse bind
 * matrices. Strings are a byte count and UTF-8; float arrays a count and floats.
 */
public object SkinFormat : AssetLoader<SkinAsset> {
  override val type: KClass<SkinAsset> = SkinAsset::class

  /** The bytes of [skin]. */
  public fun write(skin: SkinAsset): ByteArray {
    val out = BinaryOut().int(SKIN_MAGIC).int(VERSION).int(skin.joints.size)
    skin.joints.indices.forEach { out.string(skin.joints[it]).string(skin.jointNames[it]) }
    return out.string(skin.skeleton ?: NONE).floats(skin.inverseBindMatrices).toByteArray()
  }

  override fun load(guid: Guid, bytes: ByteArray): SkinAsset {
    val input = BinaryIn(bytes)
    header(input, SKIN_MAGIC, "skin")
    val count = input.count()
    val joints = ArrayList<String>(count)
    val names = ArrayList<String>(count)
    repeat(count) {
      joints += input.string()
      names += input.string()
    }
    val skeleton = input.string().ifEmpty { null }
    return SkinAsset(joints, names, input.floats(), skeleton)
  }
}

/**
 * The cooked clip file (`.clip`), little-endian: magic `ELIP`, version, name, duration, the channel
 * count, then each channel's node id, path and interpolation (their ordinals), times and values.
 */
public object ClipFormat : AssetLoader<ClipAsset> {
  override val type: KClass<ClipAsset> = ClipAsset::class

  /** The bytes of [clip]. */
  public fun write(clip: ClipAsset): ByteArray {
    val out = BinaryOut().int(CLIP_MAGIC).int(VERSION)
    out.string(clip.name).float(clip.duration)
    out.int(clip.channels.size)
    clip.channels.forEach {
      out.string(it.node).int(it.path.ordinal).int(it.interpolation.ordinal)
      out.floats(it.times).floats(it.values)
    }
    return out.toByteArray()
  }

  override fun load(guid: Guid, bytes: ByteArray): ClipAsset {
    val input = BinaryIn(bytes)
    header(input, CLIP_MAGIC, "clip")
    val name = input.string()
    val duration = input.float()
    val channels =
      List(input.count()) {
        ClipChannel(
          input.string(),
          ChannelPath.entries[input.int()],
          Interpolation.entries[input.int()],
          input.floats(),
          input.floats(),
        )
      }
    return ClipAsset(name, duration, channels)
  }
}

private fun header(input: BinaryIn, magic: Int, what: String) {
  require(input.int() == magic) { "not a cooked $what" }
  val version = input.int()
  require(version == VERSION) { "cooked $what version $version, this build reads $VERSION" }
}

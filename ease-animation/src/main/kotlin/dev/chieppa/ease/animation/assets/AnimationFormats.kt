package dev.chieppa.ease.animation.assets

import dev.chieppa.ease.assets.Guid
import dev.chieppa.ease.assets.loading.AssetLoader
import kotlin.reflect.KClass

private const val SKIN_MAGIC = 0x4E4B5345 // "ESKN"
private const val CLIP_MAGIC = 0x50494C45 // "ELIP"
private const val SKIN_VERSION = 1
private const val CLIP_VERSION = 2

/** The first clip version that carries events. */
private const val CLIP_EVENTS_VERSION = 2
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
    val out = BinaryOut().int(SKIN_MAGIC).int(SKIN_VERSION).int(skin.joints.size)
    skin.joints.indices.forEach { out.string(skin.joints[it]).string(skin.jointNames[it]) }
    return out.string(skin.skeleton ?: NONE).floats(skin.inverseBindMatrices).toByteArray()
  }

  override fun load(guid: Guid, bytes: ByteArray): SkinAsset {
    val input = BinaryIn(bytes)
    header(input, SKIN_MAGIC, SKIN_VERSION, "skin")
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
 * count, then each channel's node id, path and interpolation (their ordinals), times and values,
 * then the event count and each event's name and time.
 */
public object ClipFormat : AssetLoader<ClipAsset> {
  override val type: KClass<ClipAsset> = ClipAsset::class

  /** The bytes of [clip]. */
  public fun write(clip: ClipAsset): ByteArray {
    val out = BinaryOut().int(CLIP_MAGIC).int(CLIP_VERSION)
    out.string(clip.name).float(clip.duration)
    out.int(clip.channels.size)
    clip.channels.forEach {
      out.string(it.node).int(it.path.ordinal).int(it.interpolation.ordinal)
      out.floats(it.times).floats(it.values)
    }
    out.int(clip.events.size)
    clip.events.forEach { out.string(it.name).float(it.time) }
    return out.toByteArray()
  }

  override fun load(guid: Guid, bytes: ByteArray): ClipAsset {
    val input = BinaryIn(bytes)
    val version = header(input, CLIP_MAGIC, CLIP_VERSION, "clip")
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
    val events =
      if (version < CLIP_EVENTS_VERSION) emptyList()
      else List(input.count()) { ClipEvent(input.string(), input.float()) }
    return ClipAsset(name, duration, channels, events)
  }
}

/** Reads a cooked file's magic and version, up to [newest], and returns the version. */
private fun header(input: BinaryIn, magic: Int, newest: Int, what: String): Int {
  require(input.int() == magic) { "not a cooked $what" }
  val version = input.int()
  require(version in 1..newest) { "cooked $what version $version, this build reads 1 to $newest" }
  return version
}

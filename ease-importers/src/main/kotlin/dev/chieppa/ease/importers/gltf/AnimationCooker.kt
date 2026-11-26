package dev.chieppa.ease.importers.gltf

import dev.chieppa.ease.animation.assets.ChannelPath
import dev.chieppa.ease.animation.assets.ClipAsset
import dev.chieppa.ease.animation.assets.ClipChannel
import dev.chieppa.ease.animation.assets.ClipEvent
import dev.chieppa.ease.animation.assets.Interpolation
import dev.chieppa.ease.animation.assets.SkinAsset
import dev.chieppa.ease.gltf.GltfFile
import dev.chieppa.ease.gltf.fail

private const val MAT4 = 16
private const val CUBIC_PARTS = 3

/** Floats from one diagonal entry of a 4x4 matrix to the next. */
private const val DIAGONAL_STEP = 5

/**
 * Turns glTF skins and animations into skin and clip assets whose joints and channels name the
 * prefab's nodes by local id ([NodeIds]). A skin without inverse bind matrices gets identities.
 */
internal class AnimationCooker(private val file: GltfFile) {
  /** The asset of skin [index]. */
  fun skin(index: Int): SkinAsset {
    val path = "skins[$index]"
    val skin = file.root.skins[index]
    skin.joints.forEach { node(it, "$path.joints") }
    val matrices =
      skin.inverseBindMatrices?.let { accessor ->
        val values = file.accessors.floats(accessor, "$path.inverseBindMatrices")
        if (values.components != MAT4 || values.count < skin.joints.size) {
          fail("$path.inverseBindMatrices", "needs ${skin.joints.size} MAT4")
        }
        values.values.copyOf(skin.joints.size * MAT4)
      } ?: identities(skin.joints.size)
    return SkinAsset(
      skin.joints.map { NodeIds.node(it).text },
      skin.joints.map { file.root.nodes[it].name ?: "node$it" },
      matrices,
      skin.skeleton?.let { node(it, "$path.skeleton") }?.let { NodeIds.node(it).text },
    )
  }

  /** The clip of animation [index], called [name], firing [events]. */
  fun clip(index: Int, name: String, events: List<ClipEvent> = emptyList()): ClipAsset {
    val path = "animations[$index]"
    val animation = file.root.animations[index]
    var duration = 0f
    val channels =
      animation.channels.mapIndexedNotNull { c, channel ->
        val at = "$path.channels[$c]"
        val target = channel.target.node ?: return@mapIndexedNotNull null
        node(target, "$at.target.node")
        val sampler =
          animation.samplers.getOrNull(channel.sampler)
            ?: fail("$at.sampler", "names sampler ${channel.sampler}")
        val samplerPath = "$path.samplers[${channel.sampler}]"
        val times = file.accessors.floats(sampler.input, "$samplerPath.input").values
        val values = file.accessors.floats(sampler.output, "$samplerPath.output").values
        val interpolation = interpolation(sampler.interpolation, "$samplerPath.interpolation")
        val channelPath = channelPath(channel.target.path, "$at.target.path")
        checkCounts(times.size, values.size, channelPath, interpolation, samplerPath)
        times.lastOrNull()?.let { duration = maxOf(duration, it) }
        ClipChannel(NodeIds.node(target).text, channelPath, interpolation, times, values)
      }
    return ClipAsset(animation.name ?: name, duration, channels, events)
  }

  private fun checkCounts(
    keys: Int,
    values: Int,
    path: ChannelPath,
    interpolation: Interpolation,
    samplerPath: String,
  ) {
    val perKey = if (interpolation == Interpolation.CUBICSPLINE) CUBIC_PARTS else 1
    val fits =
      if (path == ChannelPath.WEIGHTS) keys > 0 && values % (keys * perKey) == 0
      else values == keys * perKey * path.floats
    if (!fits) fail("$samplerPath.output", "$values values do not fit $keys keys")
  }

  private fun interpolation(name: String, path: String): Interpolation =
    Interpolation.entries.firstOrNull { it.name == name }
      ?: fail(path, "'$name' is not a glTF interpolation")

  private fun channelPath(name: String, path: String): ChannelPath =
    ChannelPath.entries.firstOrNull { it.name.lowercase() == name }
      ?: fail(path, "'$name' is not a glTF animation path")

  private fun node(index: Int, path: String): Int {
    if (index !in file.root.nodes.indices) {
      fail(path, "names node $index of ${file.root.nodes.size}")
    }
    return index
  }

  private fun identities(count: Int): FloatArray =
    FloatArray(count * MAT4) { if (it % MAT4 % DIAGONAL_STEP == 0) 1f else 0f }
}

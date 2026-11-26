package dev.chieppa.ease.gltf.json

import dev.chieppa.ease.annotations.Doc
import kotlinx.serialization.Serializable

/** An animation: channels that drive node properties from keyframe samplers. */
@Doc("An animation: channels that drive node properties from keyframe samplers.")
@Serializable
public class AnimationJson(
  @Doc("The glTF property channels.") public val channels: List<ChannelJson>,
  @Doc("The glTF property samplers.") public val samplers: List<AnimationSamplerJson>,
  @Doc("The glTF property name.") public val name: String? = null,
)

/** Which [sampler] drives which [target] property. */
@Doc("Which sampler drives which target property.")
@Serializable
public class ChannelJson(
  @Doc("The glTF property sampler.") public val sampler: Int,
  @Doc("The glTF property target.") public val target: TargetJson,
)

/** A node property (`translation`, `rotation`, `scale` or `weights`); no node targets nothing. */
@Doc("A node property (translation, rotation, scale or weights); no node targets nothing.")
@Serializable
public class TargetJson(
  @Doc("The glTF property path.") public val path: String,
  @Doc("The glTF property node.") public val node: Int? = null,
)

/** Keyframe times ([input]) and values ([output]) with how to blend between them. */
@Doc("Keyframe times (input) and values (output) with how to blend between them.")
@Serializable
public class AnimationSamplerJson(
  @Doc("The glTF property input.") public val input: Int,
  @Doc("The glTF property output.") public val output: Int,
  @Doc("The glTF property interpolation.") public val interpolation: String = "LINEAR",
)

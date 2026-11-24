package dev.chieppa.ease.commands

import com.github.quillraven.fleks.World
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A shade, to check enums cross the wire. */
internal enum class Shade {
  DIM,
  BRIGHT,
}

/** A command holding one field of every kind the binary form writes. */
@Serializable
@SerialName("test.Sampler")
internal data class Sampler(
  val flag: Boolean,
  val small: Byte,
  val medium: Short,
  val letter: Char,
  val count: Int,
  val big: Long,
  val ratio: Float,
  val precise: Double,
  val label: String,
  val shade: Shade,
  val maybe: Int?,
  val missing: String?,
  val list: List<Int>,
  val map: Map<String, Long>,
  val nested: List<Sampler>,
) : Command {
  override fun apply(world: World): Command = this
}

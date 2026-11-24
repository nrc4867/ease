package dev.chieppa.ease.commands

import com.github.quillraven.fleks.World
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Paints [count] cells; its inverse lists every cell, so a large paint has a large undo, as a tile
 * edit over a big map would.
 */
@Serializable
@SerialName("test.Paint")
internal class Paint(val count: Int, val cells: List<Int> = emptyList()) : Command {
  override fun apply(world: World): Command =
    if (cells.isEmpty()) Paint(0, List(count) { it }) else Paint(cells.size)
}

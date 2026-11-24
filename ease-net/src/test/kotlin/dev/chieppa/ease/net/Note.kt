package dev.chieppa.ease.net

import com.github.quillraven.fleks.World
import dev.chieppa.ease.commands.Command
import dev.chieppa.ease.commands.CommandScope
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A line any peer may send, with marks; it changes nothing. */
@Serializable
@SerialName("test.Note")
internal class Note(val text: String, val marks: List<Int> = emptyList()) : Command {
  override val scope: CommandScope
    get() = CommandScope.ANY_PEER

  override fun apply(world: World): Command = this
}

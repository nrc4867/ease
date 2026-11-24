package dev.chieppa.ease.commands

import com.github.quillraven.fleks.World

/**
 * Applies commands to one world with undo and redo, and logs every change, undo and redo included.
 * Commands and their inverses are kept in the binary form and read fresh before each apply, so a
 * command never shares objects with the world it changed.
 */
public class CommandHistory(
  private val world: World,
  private val codec: WorldCodec,
  public val log: CommandLog = CommandLog(codec),
) {
  private val undoStack = ArrayDeque<Recorded>()
  private val redoStack = ArrayDeque<Recorded>()

  /** Whether there is a change to undo. */
  public val canUndo: Boolean
    get() = undoStack.isNotEmpty()

  /** Whether there is an undone change to redo. */
  public val canRedo: Boolean
    get() = redoStack.isNotEmpty()

  /** Who sent the change [undo] would revert, or null when there is none. */
  public val nextUndoOrigin: CommandOrigin?
    get() = undoStack.lastOrNull()?.origin

  /**
   * Applies [command] at [tick] for [origin] and makes it the next change to undo. Clears redo.
   * Throws [CommandRejected], changing nothing, when the command cannot apply.
   */
  public fun execute(command: Command, tick: Long, origin: CommandOrigin) {
    val inverse = applyLogged(codec.commandToBinary(command), tick, origin)
    undoStack.addLast(Recorded(inverse, origin))
    redoStack.clear()
  }

  /**
   * Reverts the last change, logged at [tick] for [origin]. Returns false when there is none.
   * Throws [CommandRejected], changing nothing, when the change cannot be reverted, such as after
   * another peer took authority over its entity; that change is dropped, so the next undo reaches
   * the one before it.
   */
  public fun undo(tick: Long, origin: CommandOrigin): Boolean =
    move(undoStack, redoStack, tick, origin)

  /**
   * Applies the last undone change again. Returns false when there is none. Throws
   * [CommandRejected] as [undo] does, dropping that change.
   */
  public fun redo(tick: Long, origin: CommandOrigin): Boolean =
    move(redoStack, undoStack, tick, origin)

  /**
   * Takes the top of [from], applies it and pushes its inverse onto [to]. A rejected entry stays
   * off both stacks, so a change that can never apply again does not block the ones under it.
   */
  private fun move(
    from: ArrayDeque<Recorded>,
    to: ArrayDeque<Recorded>,
    tick: Long,
    origin: CommandOrigin,
  ): Boolean {
    val recorded = from.removeLastOrNull() ?: return false
    val inverse = applyLogged(recorded.encoded, tick, origin)
    to.addLast(Recorded(inverse, recorded.origin))
    return true
  }

  private fun applyLogged(encoded: ByteArray, tick: Long, origin: CommandOrigin): ByteArray {
    val inverse = codec.commandFromBinary(encoded).apply(world)
    log.append(tick, origin, encoded)
    return codec.commandToBinary(inverse)
  }

  /** A change on a stack: the command that applies it, and who made the original change. */
  private class Recorded(val encoded: ByteArray, val origin: CommandOrigin)
}

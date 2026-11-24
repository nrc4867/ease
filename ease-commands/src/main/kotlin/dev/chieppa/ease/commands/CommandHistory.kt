package dev.chieppa.ease.commands

import com.github.quillraven.fleks.World

/**
 * Applies commands to one world with undo and redo, and logs every change, undo and redo included.
 * Only changes from an [undoable][CommandOrigin.undoable] origin enter the undo history; others,
 * such as a peer's command or an asset reload, apply and are logged but never sit between the user
 * and their own edits. Commands and their inverses are kept in the binary form and read fresh
 * before each apply, so a command never shares objects with the world it changed.
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
   * Applies [command] at [tick] for [origin]. From an undoable origin it becomes the next change to
   * undo and clears redo; from any other, undo and redo are left as they were. Throws
   * [CommandRejected], changing nothing, when the command cannot apply.
   */
  public fun execute(command: Command, tick: Long, origin: CommandOrigin) {
    val inverse = applyLogged(codec.commandToBinary(command), tick, origin)
    if (!origin.undoable) return
    undoStack.addLast(Recorded(codec.commandToBinary(inverse), origin))
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
    to.addLast(Recorded(codec.commandToBinary(inverse), recorded.origin))
    return true
  }

  /** Applies the command [encoded] holds, logs it, and returns its inverse. */
  private fun applyLogged(encoded: ByteArray, tick: Long, origin: CommandOrigin): Command {
    val inverse = codec.commandFromBinary(encoded).apply(world)
    log.append(tick, origin, encoded)
    return inverse
  }

  /** A change on a stack: the command that applies it, and who made the original change. */
  private class Recorded(val encoded: ByteArray, val origin: CommandOrigin)
}

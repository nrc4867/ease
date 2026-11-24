package dev.chieppa.ease.ecs

import com.github.quillraven.fleks.IntervalSystem

/** Appends its name to a log each tick. Fleks allows one instance per class, so each is a class. */
internal open class RecordingSystem(
  private val name: String,
  private val log: MutableList<String>,
) : IntervalSystem() {
  override fun onTick() {
    log += name
  }
}

internal class RecordsA(log: MutableList<String>) : RecordingSystem("a", log)

internal class RecordsB(log: MutableList<String>) : RecordingSystem("b", log)

internal class RecordsC(log: MutableList<String>) : RecordingSystem("c", log)

internal class RecordsD(log: MutableList<String>) : RecordingSystem("d", log)

internal class RecordsE(log: MutableList<String>) : RecordingSystem("e", log)

package dev.chieppa.ease.build.game

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

/** Writes the engine's detekt rules, which a game is held to, to [output]. */
@CacheableTask
abstract class WriteDetektConfig : DefaultTask() {
  /** The file written. */
  @get:OutputFile abstract val output: RegularFileProperty

  @TaskAction
  fun run() {
    val rules =
      checkNotNull(WriteDetektConfig::class.java.getResourceAsStream(RESOURCE)) {
          "The build logic ships no $RESOURCE"
        }
        .use { it.readBytes() }
    output.get().asFile.apply {
      parentFile.mkdirs()
      writeBytes(rules)
    }
  }

  private companion object {
    const val RESOURCE = "/ease/detekt.yml"
  }
}

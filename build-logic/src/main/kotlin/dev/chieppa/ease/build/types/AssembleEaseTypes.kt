package dev.chieppa.ease.build.types

import java.io.File
import java.util.zip.ZipFile
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

/**
 * Writes a game's `ease.d.ts`: the TypeScript part of every module on [classpath], the engine's and
 * the game's own, joined by [TypeFragments].
 */
@CacheableTask
abstract class AssembleEaseTypes : DefaultTask() {
  /** The game's runtime classpath: class directories and jars. */
  @get:Classpath abstract val classpath: ConfigurableFileCollection

  /** The file written. */
  @get:OutputFile abstract val output: RegularFileProperty

  @TaskAction
  fun run() {
    val parts = classpath.files.mapNotNull { partOf(it) }
    val target = output.get().asFile
    target.parentFile.mkdirs()
    target.writeText(TypeFragments.assemble(parts))
  }

  private fun partOf(entry: File): String? =
    when {
      entry.isDirectory -> entry.resolve(TypeFragments.RESOURCE).takeIf { it.isFile }?.readText()
      entry.isFile && entry.extension in ARCHIVES -> fromArchive(entry)
      else -> null
    }

  private fun fromArchive(archive: File): String? =
    ZipFile(archive).use { zip ->
      zip.getEntry(TypeFragments.RESOURCE)?.let {
        zip.getInputStream(it).bufferedReader().readText()
      }
    }

  private companion object {
    val ARCHIVES = setOf("jar", "zip")
  }
}

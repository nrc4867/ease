package dev.chieppa.ease.cli

import java.io.File
import java.nio.file.Files
import java.nio.file.Path

/** A start script for this command line that runs from the test's own classpath. */
object TestLauncher {
  /** Writes the script under [folder] and returns its path written with `/`, as git runs it. */
  fun script(folder: Path): String {
    val java = Path.of(System.getProperty("java.home"), "bin", "java").toString().slashed()
    val classpath = System.getProperty("java.class.path").slashed()
    val script = folder.resolve("ease-test.sh")
    Files.writeString(
      script,
      "#!/bin/sh\nexec '$java' -cp '$classpath' dev.chieppa.ease.cli.MainKt \"\$@\"\n",
    )
    script.toFile().setExecutable(true)
    return script.toString().slashed()
  }

  private fun String.slashed(): String = replace(File.separatorChar, '/')
}

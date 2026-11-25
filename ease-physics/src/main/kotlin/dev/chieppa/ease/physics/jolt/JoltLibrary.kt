package dev.chieppa.ease.physics.jolt

import com.github.stephengold.joltjni.Jolt
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.HexFormat

/**
 * Loads jolt-jni's native library once per process, from the natives jar on the classpath, and
 * readies Jolt: its allocator, factory and types. The library is copied out of the jar into a
 * folder named by its content hash under the temporary directory, so each version is written once
 * and processes running at the same time share it.
 */
internal object JoltLibrary {
  private const val PACKAGE = "com/github/stephengold"

  @Volatile private var loaded = false

  /** Loads and readies Jolt unless this process already did. */
  fun load() {
    if (loaded) return
    synchronized(this) {
      if (loaded) return
      System.load(extract().toString())
      Jolt.registerDefaultAllocator()
      Jolt.installDefaultAssertCallback()
      Jolt.installDefaultTraceCallback()
      check(Jolt.newFactory()) { "Jolt's factory could not be made" }
      Jolt.registerTypes()
      loaded = true
    }
  }

  private fun extract(): Path {
    val resource = resourcePath()
    val bytes =
      checkNotNull(JoltLibrary::class.java.classLoader.getResourceAsStream(resource)) {
          "jolt-jni's natives for this platform ($resource) are not on the classpath"
        }
        .use { it.readAllBytes() }
    val hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes))
    val folder = Path.of(System.getProperty("java.io.tmpdir"), "ease-natives", "jolt-$hash")
    val target = folder.resolve(resource.substringAfterLast('/'))
    if (!Files.isRegularFile(target) || Files.size(target) != bytes.size.toLong()) {
      Files.createDirectories(folder)
      val partial = Files.createTempFile(folder, "joltjni", ".partial")
      Files.write(partial, bytes)
      try {
        Files.move(partial, target, StandardCopyOption.ATOMIC_MOVE)
      } catch (e: java.io.IOException) {
        // Another process wrote it first, and may have it loaded; its copy is the same bytes.
        Files.deleteIfExists(partial)
        if (!Files.isRegularFile(target)) throw e
      }
    }
    return target
  }

  /** Where jolt-jni's natives jar keeps the library for this machine. */
  private fun resourcePath(): String {
    val os = System.getProperty("os.name").lowercase()
    val arch = System.getProperty("os.arch").lowercase()
    val cpu = if (arch == "aarch64" || arch == "arm64") "aarch64" else "x86-64"
    return when {
      os.startsWith("windows") -> "windows/$cpu/$PACKAGE/joltjni.dll"
      os.startsWith("mac") -> "osx/$cpu/$PACKAGE/libjoltjni.dylib"
      else -> "linux/$cpu/$PACKAGE/libjoltjni.so"
    }
  }
}

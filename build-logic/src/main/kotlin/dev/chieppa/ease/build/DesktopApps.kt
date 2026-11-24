package dev.chieppa.ease.build

import org.gradle.api.tasks.JavaExec

/**
 * Sets up [task] to run a desktop app that opens windows: native access for the FFM bindings and
 * LWJGL, LWJGL's memory access without warnings, and on a Mac the main thread as the first thread,
 * which Cocoa needs for windows.
 */
fun desktopApp(task: JavaExec) {
  task.jvmArgs(
    "--enable-native-access=ALL-UNNAMED",
    "--sun-misc-unsafe-memory-access=allow",
  )
  if (NativePlatform.hostIsMac()) task.jvmArgs("-XstartOnFirstThread")
}

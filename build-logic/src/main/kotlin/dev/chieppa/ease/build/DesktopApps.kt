package dev.chieppa.ease.build

import org.gradle.api.tasks.JavaExec

/**
 * Sets up [task] to run a desktop app that opens windows: native access for the FFM bindings and
 * LWJGL, LWJGL's memory access without warnings, and on a Mac the main thread as the first thread,
 * which Cocoa needs for windows.
 */
fun desktopApp(task: JavaExec) {
  headlessApp(task)
  task.jvmArgs("--sun-misc-unsafe-memory-access=allow")
  if (NativePlatform.hostIsMac()) task.jvmArgs("-XstartOnFirstThread")
}

/**
 * Sets up [task] to run a host with no window: native access for the FFM bindings and the JNI
 * libraries (Jolt), so JDK 25 prints no restricted-method warnings.
 */
fun headlessApp(task: JavaExec) {
  task.jvmArgs("--enable-native-access=ALL-UNNAMED")
}

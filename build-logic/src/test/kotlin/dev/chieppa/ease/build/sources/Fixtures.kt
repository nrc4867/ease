package dev.chieppa.ease.build.sources

/** A main Kotlin source holding [lines]. */
fun mainSource(vararg lines: String, path: String = "ease-ecs/src/main/kotlin/A.kt") =
  SourceFile(path, lines.toList())

/** A test Kotlin source holding [lines]. */
fun testSource(vararg lines: String, path: String = "ease-ecs/src/test/kotlin/ATest.kt") =
  SourceFile(path, lines.toList())

/** A file at [path] with [count] blank lines. */
fun sourceOfLength(path: String, count: Int) = SourceFile(path, List(count) { "" })

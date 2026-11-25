// The module of a game that holds its GameModule: the player to run it in, and the tasks that run
// it. A game has one; its other modules apply ease.game-module alone.
import dev.chieppa.ease.build.desktopApp
import dev.chieppa.ease.build.game.GameDependencies
import dev.chieppa.ease.build.headlessApp
import dev.chieppa.ease.build.types.AssembleEaseTypes

plugins { id("ease.game-module") }

dependencies { runtimeOnly(GameDependencies.module(project, "ease-player")) }

val main = the<SourceSetContainer>()["main"]

tasks.register<JavaExec>("runGame") {
  group = "application"
  description = "Runs the game in the player until its window is closed."
  classpath = main.runtimeClasspath
  mainClass = GameDependencies.PLAYER_MAIN
  desktopApp(this)
}

tasks.register<JavaExec>("runHeadless") {
  group = "application"
  description = "Runs the game in the player with no window or GPU for 600 ticks."
  classpath = main.runtimeClasspath
  mainClass = GameDependencies.PLAYER_MAIN
  args("--headless", "600")
  headlessApp(this)
}

// The game's ease.d.ts: its own types joined with every engine module's, for scripts and tests.
tasks.register<AssembleEaseTypes>("assembleEaseTypes") {
  group = "build"
  description = "Writes ease.d.ts from the type declarations of every module the game runs on."
  classpath.from(main.runtimeClasspath)
  output = layout.buildDirectory.file("ease/ease.d.ts")
}

import dev.chieppa.ease.build.JoltNatives
import dev.chieppa.ease.build.LwjglNatives
import dev.chieppa.ease.build.WebgpuNatives

// The player runs whichever game is on its classpath, so it has no run task of its own: a game
// takes it with runtimeOnly and runs dev.chieppa.ease.player.PlayerKt (see :sample:runSample).
plugins { id("ease.kotlin-module") }

dependencies {
  implementation(project(":ease-runtime"))
  // The native libraries for the platform this player is built and packaged for.
  runtimeOnly(
    project.dependencies.variantOf(libs.webgpu.native.ffm) {
      classifier(WebgpuNatives.classifier(providers))
    }
  )
  runtimeOnly(JoltNatives.natives(providers, libs.versions.jolt.jni.get()))
  // SDL for windows and input; stb_image and tinyexr for the textures a game's content imports.
  for (library in
    listOf(libs.lwjgl.asProvider(), libs.lwjgl.sdl, libs.lwjgl.stb, libs.lwjgl.tinyexr)) {
    runtimeOnly(
      project.dependencies.variantOf(library) { classifier(LwjglNatives.classifier(providers)) }
    )
  }
}

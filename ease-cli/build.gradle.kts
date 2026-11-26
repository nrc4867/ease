import dev.chieppa.ease.build.LwjglNatives

// The ease command line: new projects from templates and generators for what a project holds.
// Later commands join it here. `./gradlew :ease-cli:installDist` writes the distribution under
// build/install/ease; `./gradlew :ease-cli:runEase --args="new empty --name Moon --dir ../moon"`
// runs it from Gradle.
plugins {
  id("ease.kotlin-module")
  id("org.jetbrains.kotlin.plugin.serialization")
  application
}

dependencies {
  implementation(project(":ease-annotations"))
  implementation(project(":ease-serialization"))
  implementation(project(":ease-project"))
  // Ease's texture importer, and the image decoders' natives for this machine.
  implementation(project(":ease-textures"))
  for (library in listOf(libs.lwjgl.asProvider(), libs.lwjgl.stb, libs.lwjgl.tinyexr)) {
    runtimeOnly(
      project.dependencies.variantOf(library) { classifier(LwjglNatives.classifier(providers)) }
    )
  }
  testImplementation(testFixtures(project(":ease-project")))
}

application {
  applicationName = "ease"
  mainClass = "dev.chieppa.ease.cli.MainKt"
}

// Templates and generators are folders of files in the resources, under ease-templates. The Gradle
// wrapper a generated game ships is this repository's own, so a game starts on the wrapper Ease is
// built with.
tasks.processResources {
  from(rootProject.layout.projectDirectory) {
    include("gradlew", "gradlew.bat", "gradle/wrapper/**")
    into("ease-templates/parts/wrapper")
  }
  val easeVersion = project.version.toString()
  inputs.property("easeVersion", easeVersion)
  filesMatching("ease-cli.properties") { expand("easeVersion" to easeVersion) }
}

tasks.register<JavaExec>("runEase") {
  group = "application"
  description = "Runs the ease command line; pass its arguments as --args='...'."
  classpath = sourceSets.main.get().runtimeClasspath
  mainClass = application.mainClass
  // Relative paths in the arguments are relative to where Gradle was started, not this module.
  workingDir = gradle.startParameter.currentDir
}

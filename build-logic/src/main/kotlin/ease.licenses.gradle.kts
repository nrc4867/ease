// The license check, applied to the root project: every module's runtime classpath, except the
// modules that never ship, must use a permissive license, and NOTICE lists each shipped dependency.
import dev.chieppa.ease.build.licenses.CheckLicenses
import dev.chieppa.ease.build.licenses.CheckNotice
import dev.chieppa.ease.build.licenses.GenerateNotice
import dev.chieppa.ease.build.licenses.LicenseTask
import dev.chieppa.ease.build.licenses.ShippedModules

plugins { base }

/** Modules whose code never ships. */
val unshipped = setOf("architecture-tests", "viewport-spike")

val shippedScope = configurations.dependencyScope("shipped")

val shippedClasspath =
  configurations.resolvable("shippedClasspath") {
    extendsFrom(shippedScope.get())
    attributes {
      attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
      attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
      attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
      attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))
      attribute(
        TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE,
        objects.named(TargetJvmEnvironment.STANDARD_JVM),
      )
    }
  }

subprojects
  .filter { it.name !in unshipped }
  .forEach { dependencies.add(shippedScope.name, project(it.path)) }

val shippedModules =
  shippedClasspath
    .flatMap { it.incoming.resolutionResult.rootComponent }
    .map { ShippedModules.licenses(project, ShippedModules.coordinates(it)) }

val licenseConfig = layout.projectDirectory.dir("config/licenses")
val noticeFile = layout.projectDirectory.file("NOTICE")

tasks.withType<LicenseTask>().configureEach {
  modules = shippedModules
  overrides = licenseConfig.file("overrides.txt")
}

val checkLicenses =
  tasks.register<CheckLicenses>("checkLicenses") {
    group = "verification"
    description = "Checks that every shipped dependency uses a permissive license."
    report = layout.buildDirectory.file("licenses/report.txt")
  }

val generateNotice =
  tasks.register<GenerateNotice>("generateNotice") {
    group = "documentation"
    description = "Writes NOTICE: Ease's notice and every shipped dependency with its license."
    header = licenseConfig.file("notice-header.txt")
    notice = noticeFile
  }

val checkNotice =
  tasks.register<CheckNotice>("checkNotice") {
    group = "verification"
    description = "Checks that NOTICE matches what generateNotice writes."
    header = licenseConfig.file("notice-header.txt")
    notice = noticeFile
    report = layout.buildDirectory.file("licenses/notice.txt")
    mustRunAfter(generateNotice)
  }

tasks.named("check") { dependsOn(checkLicenses, checkNotice) }

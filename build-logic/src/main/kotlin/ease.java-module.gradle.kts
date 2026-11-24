// A Java module, for the hot core: JDK 25 and google-java-format.
import dev.chieppa.ease.build.catalogVersion

plugins {
  `java-library`
  id("ease.module")
}

java {
  toolchain {
    languageVersion = JavaLanguageVersion.of(catalogVersion("jvmToolchain"))
  }
}

spotless {
  java { googleJavaFormat(catalogVersion("google-java-format")) }
  kotlinGradle { ktfmt(catalogVersion("ktfmt")).googleStyle() }
}

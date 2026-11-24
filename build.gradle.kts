plugins { id("ease.root") }

allprojects {
  group = "dev.chieppa.ease"
  version = providers.gradleProperty("ease.version").getOrElse("0.1.0-SNAPSHOT")
}

plugins { id("ease.root") }

allprojects {
  group = "dev.chieppa.ease"
  version = providers.gradleProperty("ease.version").get()
}

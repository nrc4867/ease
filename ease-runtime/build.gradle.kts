plugins { id("ease.kotlin-module") }

dependencies {
  api(project(":ease-assets"))
  api(project(":ease-render"))
  api(project(":ease-importers"))
  api(project(":ease-commands"))
  api(project(":ease-net"))
  testImplementation(project(":ease-testing"))
}

plugins { id("ease.kotlin-module") }

dependencies {
  api(project(":ease-assets"))
  api(project(":ease-render"))
  api(project(":ease-commands"))
  testImplementation(project(":ease-testing"))
}

// A whole project as git holds it: the checks over every file (`ease check`), the mechanical fixes,
// the merge driver and the git setup `ease new` writes. Hosts such as the CLI and the editor use
// it.
plugins {
  id("ease.kotlin-module")
  // TestGit and TestProject, for this module's tests and the CLI's.
  `java-test-fixtures`
}

dependencies {
  api(project(":ease-scene"))
  api(project(":ease-assets"))
  api(project(":ease-serialization"))
  testFixturesApi(project(":ease-scene"))
  testFixturesApi(project(":ease-assets"))
}

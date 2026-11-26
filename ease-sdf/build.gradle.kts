// Distance fields on the CPU: shapes built from code with transforms and smooth CSG, compiled into
// one program evaluated in Kotlin, and surface nets meshing. Depends only on ease-math, so any
// layer above it can use it; ease-sdf-gpu adds the GPU side.
plugins { id("ease.kotlin-module") }

dependencies {
  api(project(":ease-math"))
  testImplementation(project(":ease-testing"))
}

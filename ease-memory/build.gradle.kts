// Off-heap memory the GPU, import and physics code share: the per-frame arena, cursor writers,
// mapped files, and the @GpuStruct annotation ease-codegen writes struct writers from. No
// dependencies.
plugins { id("ease.kotlin-module") }

dependencies { testImplementation(project(":ease-testing")) }

package dev.chieppa.ease.build.layers

/**
 * The layer of every engine and game module. A dependency on or from a module missing here fails
 * the build.
 */
val moduleLayers: Map<String, Layer> =
  mapOf(
    "ease-codegen" to Layer.BUILD_TOOL,
    "ease-testing" to Layer.TEST_SUPPORT,
    "ease-math" to Layer.FOUNDATION,
    "ease-annotations" to Layer.FOUNDATION,
    "ease-memory" to Layer.FOUNDATION,
    "ease-ecs" to Layer.CORE,
    "ease-serialization" to Layer.DATA,
    "ease-sdf" to Layer.DATA,
    "ease-commands" to Layer.WORLD,
    "ease-platform" to Layer.PLATFORM,
    "ease-assets" to Layer.ASSETS,
    "ease-textures" to Layer.MEDIA,
    "ease-scene" to Layer.SCENE,
    "ease-physics" to Layer.PHYSICS,
    "ease-project" to Layer.PROJECT,
    "ease-net" to Layer.NET,
    "ease-gpu" to Layer.GPU,
    "ease-render" to Layer.RENDER,
    "ease-sdf-gpu" to Layer.RENDER,
    "ease-runtime" to Layer.RUNTIME,
    "ease-player" to Layer.HOST,
    "ease-agent" to Layer.HOST,
    "ease-cli" to Layer.HOST,
    "viewport-spike" to Layer.HOST,
    "sample" to Layer.GAME,
  )

/** The package a module's code lives in, such as `dev.chieppa.ease.math` for `ease-math`. */
fun modulePackage(module: String): String =
  "dev.chieppa.ease." + module.removePrefix("ease-").replace('-', '.')

/** The layer of [module], or an error naming where to add it. */
fun layerOf(module: String): Layer =
  moduleLayers[module]
    ?: error("Module $module has no layer; add it to moduleLayers in build-logic")

/**
 * Why [from] may not depend on [to], or null when it may: only a module in a strictly lower layer
 * is allowed, or the module itself, as its tests take its own test fixtures.
 */
fun layerViolation(from: String, to: String): String? {
  if (from == to) return null
  val fromLayer = layerOf(from)
  val toLayer = layerOf(to)
  if (toLayer < fromLayer) return null
  return "$from (${fromLayer.name}) may not depend on $to (${toLayer.name}): " +
    "a module depends only on modules in lower layers"
}

/**
 * Why [from] may not take [to] in [configuration], or null when it may: a test-support module is
 * only for the configurations of test source sets, whose names start with `test`.
 */
fun testSupportViolation(from: String, to: String, configuration: String): String? {
  if (layerOf(to) != Layer.TEST_SUPPORT || configuration.startsWith("test")) return null
  return "$from may take $to only in a test configuration, not $configuration: " +
    "test-support modules never reach main code"
}

/**
 * Why [from] may not take [to] in [configuration], or null when it may: a build-tool module is only
 * for the configurations of symbol processing, whose names start with `ksp`.
 */
fun buildToolViolation(from: String, to: String, configuration: String): String? {
  if (layerOf(to) != Layer.BUILD_TOOL || configuration.startsWith("ksp")) return null
  return "$from may take $to only in a ksp configuration, not $configuration: " +
    "build tools run inside the build and never reach a classpath"
}

/** The test-support modules, comma-separated, for tests that check imports against them. */
fun testSupportProperty(): String =
  moduleLayers.filterValues { it == Layer.TEST_SUPPORT }.keys.joinToString(",")

/**
 * The layers as `module:rank:package` triples, lowest rank first, for tests that check imports
 * against them.
 */
fun layersProperty(): String =
  moduleLayers.entries.joinToString(",") { (module, layer) ->
    "$module:${layer.ordinal}:${modulePackage(module)}"
  }

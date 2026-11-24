package dev.chieppa.ease.build.layers

/**
 * The layer of every engine and game module. A dependency on or from a module missing here fails
 * the build.
 */
val moduleLayers: Map<String, Layer> =
  mapOf(
    "ease-testing" to Layer.TEST_SUPPORT,
    "ease-math" to Layer.FOUNDATION,
    "ease-ecs" to Layer.CORE,
    "ease-platform" to Layer.PLATFORM,
    "ease-gpu" to Layer.GPU,
    "ease-render" to Layer.RENDER,
    "ease-runtime" to Layer.RUNTIME,
    "ease-player" to Layer.HOST,
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
 * is allowed.
 */
fun layerViolation(from: String, to: String): String? {
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

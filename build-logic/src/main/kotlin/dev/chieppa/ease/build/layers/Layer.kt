package dev.chieppa.ease.build.layers

/** A layer of modules, lowest first. A module may depend only on modules in lower layers. */
enum class Layer {
  /**
   * Code that runs only inside the build, such as annotation processors. Only `ksp` configurations
   * may take them, so they never reach a classpath.
   */
  BUILD_TOOL,

  /** Helpers for tests, such as the allocation harness. Only test source sets may use them. */
  TEST_SUPPORT,

  /** Plain value types and annotations every other module may use. */
  FOUNDATION,

  /** The world model: entities, components, systems. */
  CORE,

  /** Data formats every file the engine writes shares, such as canonical YAML. */
  DATA,

  /** Changes to a world: commands, undo, the command log and their formats. */
  WORLD,

  /** Windows, input, audio output, files, clocks. */
  PLATFORM,

  /**
   * Asset identity, the asset database, loaders, file watching and the file association service.
   */
  ASSETS,

  /** Cooked media formats, such as textures, with their importers and loaders. */
  MEDIA,

  /** Nodes, scenes, prefabs and sublevels, their merge-safe files and their reloads. */
  SCENE,

  /** Jolt bodies, collision layers, surfaces, queries, volumes and the character controller. */
  PHYSICS,

  /**
   * Skeletons, clips, blending and state machines; poses, sockets, animation events and the probes
   * they run.
   */
  ANIMATION,

  /** Whole projects as git holds them: checks over every file, fixes, the merge driver. */
  PROJECT,

  /** Sessions between peers: channels, snapshots, inputs and the authority's command checks. */
  NET,

  /** WebGPU device, surfaces, pipelines. */
  GPU,

  /** The render graph and everything drawn through it. */
  RENDER,

  /**
   * The engine's importers: source files such as glTF turned into the cooked assets the layers
   * below load.
   */
  IMPORT,

  /** Worlds, ticking, views and game modules, for every host. */
  RUNTIME,

  /** Programs that host the runtime: the player, later the editor. */
  HOST,

  /** Games built on the engine. Nothing in the engine depends on them. */
  GAME,
}

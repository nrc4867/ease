# Ease high-level design

Ease is a game engine: a set of libraries, an editor built on them, and games that are separate projects. Each file below covers one topic.

## overview/
- `overview/intro.md`: what Ease is, the engine and game repos, how priorities flow.
- `overview/squash-consumer.md`: Squash as a third consumer of the engine.
- `overview/principles.md`: the rules the whole design follows.
- `overview/dependency-rules.md`: module dependency direction and its build check.
- `overview/open-questions.md`: unresolved design questions.
- `overview/targets.md`: the floor machine and the frame, memory and load budgets.
- `overview/decisions.md`: decisions and the reasons for them.

## engine/
- `engine/layers.md`: the four module layers.
- `engine/core.md`: math, ECS, assets, jobs, serialization.
- `engine/platform.md`: platform backends and the input action layer.
- `engine/gpu-and-rendering.md`: WebGPU bindings, the render graph, lighting pipelines.
- `engine/physics-animation-audio.md`: Jolt physics, animation graph, audio module.
- `engine/module-rules.md`: what may and may not live in engine modules.

## systems/
- `systems/frame-model.md`: simulation, extract, render and audio lanes in one frame.
- `systems/materials.md`: templates, instances, permutations, bind groups.
- `systems/particles.md`: GPU particle effects and emitter modules.
- `systems/audio.md`: sound events, emitters, buses, music, haptics.
- `systems/terrain.md`: chunked terrain and the three deformation tiers.
- `systems/distance-fields.md`: the optional `ease-sdf` module.
- `systems/sequencer.md`: timeline tracks, scrubbing, playback modes.
- `systems/ui.md`: the in-game UI module.

## gameplay/
- `gameplay/overview.md`: the gameplay framework in brief.
- `gameplay/quests.md`: stages, aliases, objectives, states, actions.
- `gameplay/dialogue.md`: topics, lines and responses.
- `gameplay/conditions.md`: conditions, globals and scripting hooks.
- `gameplay/story-events.md`: the story manager and event nodes.
- `gameplay/save-load.md`: what is saved, formats, migrations.

## editor/
- `editor/shape.md`: editor modules, `GameModule` and `EditorExtension`.
- `editor/opening-a-project.md`: project loading and hot reload.
- `editor/viewport-and-play.md`: shared device viewport, edit, play and standalone modes.
- `editor/tools.md`: the built-in tools.
- `editor/agent-access.md`: files-first agent access and the fixed MCP tool set.
- `editor/plugins.md`: plugin kinds, trust, capabilities, boundaries.

## runtime/
- `runtime/hosts.md`: hosts and the runtime building blocks.
- `runtime/player.md`: the generic player launcher.
- `runtime/cooking-and-packaging.md`: collect, cook, pack, bundle.
- `runtime/headless.md`: offscreen, simulation and replay modes.
- `runtime/distribution.md`: bootstrap jar, signed installers, branded export.
- `runtime/game-packages.md`: the `.ease` package format and verification.

## networking/
- `networking/overview.md`: network-ready design and layering.
- `networking/authority.md`: authority, topologies, ownership transfer.
- `networking/replication.md`: replicated components, snapshots, commands, events.
- `networking/prediction.md`: prediction, reconciliation, interpolation, lag compensation.
- `networking/integration.md`: physics, terrain, quests and saves over the network.
- `networking/transport.md`: transports, GameNetworkingSockets, sessions.
- `networking/tooling.md`: multi-client play, simulation, bots, replays.
- `networking/open-questions.md`: unresolved networking questions.

## code-quality/
- `code-quality/rules-at-a-glance.md`: every rule and what enforces it.
- `code-quality/formatting.md`: Spotless, detekt, source checks, pre-commit hook.
- `code-quality/code-shape.md`: line, file and class shape, registration, dependency injection.
- `code-quality/testing.md`: public-behavior tests, no waits, no goldens, tiers.
- `code-quality/performance.md`: allocation budgets, benchmarks, profiling.
- `code-quality/dependencies.md`: the permissive-license rule, its check and the shipped dependencies.
- `code-quality/ci.md`: pipelines on free GitHub runners.
- `code-quality/comments-and-docs.md`: comments, Markdown rules, remove-never-deprecate.
- `code-quality/carried-from-squash.md`: Squash rules that apply unchanged.

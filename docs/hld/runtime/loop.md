# The fixed-step loop

Simulation advances in fixed steps; rendering runs as often as the host draws and blends between the last two steps. `FixedStepLoop` in `ease-runtime` (`dev.chieppa.ease.runtime.loop`) does the bookkeeping.

- **Ticks are the only time.** A `Simulation` gets `step(tick)`, with ticks counted from 0. It never reads a clock, so a step means the same thing in a test, a replay and a live game.
- **The clock is injected.** The loop reads a `Clock` from `ease-platform` (`dev.chieppa.ease.platform.time`): `SystemClock` in real hosts, `ManualClock` in tests and headless runs, which advance it by hand.
- **Each frame** runs the whole steps the time since the last frame holds, then sets `alpha`, how far the clock is toward the next step (0 to 1). The first frame only starts the clock. `stepOnce()` runs one step without the clock, for a host stepping a paused world.
- **Catch-up is capped.** After a slow frame the loop runs at most `FixedStep.maxStepsPerFrame` steps and drops the rest of the backlog (`droppedNanos` counts it), so it holds its step instead of spiralling into ever more steps. The default is 60 steps a second with a cap of 15.
- **No allocation per frame;** a test holds `frame()` to a zero budget.

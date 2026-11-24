# Target hardware and budgets

Ease is built to run well on one floor machine, a Steam Deck class handheld. These are starting numbers, revisited in Phase 6 once real scenes can be measured.

| Target | Value |
| --- | --- |
| Floor machine | Steam Deck class: 4-core Zen 2 CPU, RDNA 2 GPU with 8 compute units, 16 GB shared memory, 1280×800 |
| Frame rate | 60 fps on the floor machine at 1280×800 |
| Frame time | 16.6 ms total: simulation step ≤ 4 ms, render submission ≤ 4 ms, GPU ≤ 12 ms |
| Allocation | No allocation per tick in steady state, measured by the allocation harness; a command applied through the history at most 4 KB (about 1.8 KB measured) |
| Memory | Game process ≤ 4 GB RAM, ≤ 2 GB GPU memory |
| Load time | Cold start to menu ≤ 10 s; level or floor change ≤ 3 s |

- **Frame time** has separate CPU and GPU budgets. The simulation step and render submission share the CPU's frame (see `systems/frame-model.md` for the lanes), and the GPU's 12 ms runs alongside them; the rest of each frame is headroom.
- **Memory** is shared on the floor machine, so the RAM and GPU budgets together stay well under its 16 GB, leaving room for the operating system.
- **Allocation** is the one budget CI gates today; the others are measured on the floor machine and tracked, as `code-quality/performance.md` describes.

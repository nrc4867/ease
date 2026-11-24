# Systems and the frame

These systems are engine-generic: materials, particles, 3D audio and terrain. All four follow one rule: simulation owns the truth, and rendering and audio work from a read-only snapshot taken once per frame.

One frame has four lanes: simulation, extract, render and audio. Simulation produces the state; extract takes the read-only snapshot; render and audio each read that snapshot.

Only the simulation lane changes game state. Terrain edits land there so physics and visuals agree, while particles simulate on the GPU because nothing in gameplay reads them.

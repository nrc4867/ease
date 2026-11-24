# Prediction and latency

Players should feel no delay on their own actions and see everyone else move smoothly.

- **Prediction:** a peer simulates its own character immediately from local input instead of waiting for the authority. Inputs are kept with their tick numbers.
- **Reconciliation:** when the authority's state for a tick arrives, the peer compares it with its prediction. If they differ, it resets to the authority's state and replays the inputs since then. Small errors are smoothed visually over a few frames rather than snapped.
- **Interpolation:** entities the peer doesn't own are drawn slightly in the past (around 100 ms, tuned per game) between two received snapshots, so they move smoothly despite jitter and loss.
- **Lag compensation:** when a command says "I hit target 41", the authority briefly rewinds hit volumes to where that player saw them, then checks.
- **What predicts:** the character controller, movement abilities and cosmetic effects. What doesn't: other entities' physics, damage and anything that changes shared state.

# Authority and topologies

Every replicated entity has exactly one **authority**: the peer allowed to change its state. Everyone else holds a replica. Topology is just a policy for who gets authority, so client-server and peer-to-peer share one replication layer.

| Topology | Who has authority | Good for | Cost |
|---|---|---|---|
| Dedicated server | The server, for everything; clients own only their input | Many players, anti-cheat, persistent worlds | Someone runs servers; the headless player in simulation mode is that server |
| Host (listen server) | One player's game acts as the server | Co-op with friends, no infrastructure | Host's connection limits everyone; host advantage; game ends if the host leaves unless authority migrates |
| Distributed ownership | Each peer owns its own character and what it spawns; shared objects belong to a host or move between peers | Small co-op where each player feels zero latency on their own actions | Harder to keep consistent; trust between peers |

- **Ownership transfer:** authority over an entity can move (picking up an object, host migration). The transfer is a reliable message with a tick number; the old owner stops writing on that tick.
- **Writes are guarded:** gameplay code checks `hasAuthority` before changing replicated state. In debug builds, writing a replicated component without authority throws, so mistakes surface in the first test, not the first bug report.
- **Not in the first version:** deterministic lockstep or rollback (sending only inputs). It suits small, fast games but needs bit-identical simulation everywhere. The fixed step and Jolt's cross-platform determinism option keep it possible as a fourth policy.
